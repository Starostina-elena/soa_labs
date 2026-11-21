#!/bin/sh
set -eu
umask 077

LAB_DIR=$(CDPATH= cd -P "$(dirname "$0")" && pwd)
PAYARA_HOME="$LAB_DIR/payara6"
WILDFLY_HOME="$LAB_DIR/wildfly-35.0.1.Final"
JAVA_HOME=${JAVA_HOME:-/usr/local/openjdk17}
AS_JAVA=$JAVA_HOME
PATH="$JAVA_HOME/bin:$PATH"
PGHOST=${PGHOST:-pg}
PGPORT=${PGPORT:-5432}
PGDATABASE=${PGDATABASE:-studs}
DB_USER=${DB_USER:-s414257}
PGPASSFILE=${PGPASSFILE:-"$HOME/.pgpass"}
DB_URL="jdbc:postgresql://$PGHOST:$PGPORT/$PGDATABASE?currentSchema=s414257"
_JAVA_OPTIONS='-Xms32m -Xmx192m -Xss512k -XX:ActiveProcessorCount=2 -XX:+UseSerialGC -XX:ReservedCodeCacheSize=48m'
JAVA_OPTS='-Xms32m -Xmx192m -XX:MaxMetaspaceSize=256m -Djava.net.preferIPv4Stack=true'
lab_phase='preparation'
lab_cli_options='-Xms16m -Xmx64m -Xss512k -XX:ActiveProcessorCount=2 -XX:+UseSerialGC'
export LAB_DIR PAYARA_HOME WILDFLY_HOME JAVA_HOME AS_JAVA PATH PGHOST PGPORT PGDATABASE DB_USER PGPASSFILE DB_URL _JAVA_OPTIONS JAVA_OPTS

fail() { printf '%s\n' "$*" >&2; exit 1; }
pause() { sleep 3; }
small_java() { env _JAVA_OPTIONS="$lab_cli_options" "$@"; }
phase() { lab_phase=$1; printf '\n=== %s ===\n' "$lab_phase"; }

on_exit() {
    lab_status=$?
    if [ "$lab_status" -ne 0 ]; then
        printf '\nSetup failed at: %s. Recent server logs:\n' "$lab_phase" >&2
        tail -n 35 "$PAYARA_HOME/glassfish/domains/lab2/logs/server.log" 2>/dev/null || true
        tail -n 35 "$LAB_DIR/wildfly.log" 2>/dev/null || true
    fi
}
trap on_exit 0

test -x "$JAVA_HOME/bin/java" || fail "JDK 17 not found: $JAVA_HOME"
for lab_command in unzip psql curl sockstat nc; do
    command -v "$lab_command" >/dev/null || fail "Missing command: $lab_command"
done
for lab_file in organizations.war orgmanager.war client.war db_init.sql; do
    test -r "$LAB_DIR/$lab_file" || fail "Missing file: $LAB_DIR/$lab_file"
done
test -r "$PGPASSFILE" || fail "Missing PostgreSQL password file: $PGPASSFILE"
for lab_war in organizations orgmanager client; do
    unzip -tq "$LAB_DIR/$lab_war.war"
done

if [ ! -x "$PAYARA_HOME/bin/asadmin" ]; then
    test -r "$LAB_DIR/soa-payara.zip" || fail 'Put soa-payara.zip next to setup.sh'
    unzip -qo "$LAB_DIR/soa-payara.zip" -d "$LAB_DIR"
    rm "$LAB_DIR/soa-payara.zip"
    pause
fi
if [ ! -x "$WILDFLY_HOME/bin/standalone.sh" ]; then
    test -r "$LAB_DIR/soa-wildfly.zip" || fail 'Put soa-wildfly.zip next to setup.sh'
    unzip -qo "$LAB_DIR/soa-wildfly.zip" -d "$LAB_DIR"
    rm "$LAB_DIR/soa-wildfly.zip"
    pause
fi

stop_lab_java() {
    lab_marker=$1
    lab_logical_marker=$2
    lab_pids=$(ps -ww -U "$(id -un)" -o pid=,args= |
        awk -v marker="$lab_marker" -v logical="$lab_logical_marker" '$2 ~ /(^|\/)java$/ && (index($0, marker) || index($0, logical)) {print $1}')
    for lab_pid in $lab_pids; do
        printf 'Stopping lab Java PID %s\n' "$lab_pid"
        kill -TERM "$lab_pid" 2>/dev/null || true
        lab_wait=0
        while kill -0 "$lab_pid" 2>/dev/null; do
            lab_wait=$((lab_wait + 1))
            if [ "$lab_wait" -ge 10 ]; then
                lab_still_ours=$(ps -ww -U "$(id -un)" -o pid=,args= |
                    awk -v pid="$lab_pid" -v marker="$lab_marker" -v logical="$lab_logical_marker" '$1 == pid && $2 ~ /(^|\/)java$/ && (index($0, marker) || index($0, logical)) {print $1}')
                if [ "$lab_still_ours" = "$lab_pid" ]; then
                    printf 'Lab PID %s is stuck; forcing it to stop\n' "$lab_pid"
                    kill -KILL "$lab_pid" 2>/dev/null || true
                    pause
                fi
                if kill -0 "$lab_pid" 2>/dev/null; then
                    fail "PID $lab_pid is still present; refusing to change configuration"
                fi
                break
            fi
            pause
        done
    done
}
stop_lab_java "$PAYARA_HOME/" "$HOME/soa-lab2/payara6/"
stop_lab_java "$WILDFLY_HOME/" "$HOME/soa-lab2/wildfly-35.0.1.Final/"
pause

ports_busy() {
    if sockstat -46 -l -p "$1" | awk 'NR>1 {found=1} END {exit !found}'; then
        return 0
    fi
    for lab_probe_port in $(printf '%s' "$1" | tr ',' ' '); do
        if nc -z -w 1 127.0.0.1 "$lab_probe_port" >/dev/null 2>&1; then
            printf 'Port %s is busy; trying another range\n' "$lab_probe_port" >&2
            return 0
        fi
    done
    return 1
}
lab_base=22000
while ports_busy "$((lab_base+37)),$((lab_base+38)),$((lab_base+39)),$((lab_base+48)),$((lab_base+49)),$((lab_base+59)),$((lab_base+76)),$((lab_base+80)),$((lab_base+81)),$((lab_base+86))"; do
    lab_base=$((lab_base+100))
    test "$lab_base" -lt 30000 || fail 'No free Payara port range'
done
lab_https=$((lab_base+81))
lab_admin=$((lab_base+48))
lab_offset=30000
while ports_busy "$((lab_offset+4712)),$((lab_offset+4713)),$((lab_offset+8009)),$((lab_offset+8080)),$((lab_offset+8443)),$((lab_offset+9990)),$((lab_offset+9993))"; do
    lab_offset=$((lab_offset+1000))
    test "$lab_offset" -lt 50000 || fail 'No free WildFly port range'
done
lab_people_https=$((lab_offset+8443))
lab_people_admin=$((lab_offset+9990))
lab_origin="https://localhost:$lab_https"
lab_org_url="$lab_origin/organizations-api"
lab_people_url="https://localhost:$lab_people_https/orgmanager"
printf 'Selected HTTPS ports: organizations/client=%s, people=%s\n' "$lab_https" "$lab_people_https"

cat > "$LAB_DIR/lab-env.sh" <<EOF
export LAB_DIR='$LAB_DIR'
export PAYARA_HOME='$PAYARA_HOME'
export WILDFLY_HOME='$WILDFLY_HOME'
export JAVA_HOME='$JAVA_HOME'
export AS_JAVA='$JAVA_HOME'
export PATH='$JAVA_HOME/bin:$PATH'
export DB_URL='$DB_URL'
export DB_USER='$DB_USER'
export PGPASSFILE='$PGPASSFILE'
export _JAVA_OPTIONS='$_JAVA_OPTIONS'
export JAVA_OPTS='$JAVA_OPTS'
export ORG_API_URL='$lab_origin'
export ORG_SERVICE_URL='$lab_org_url'
export PEOPLE_API_URL='$lab_people_url'
export CLIENT_ORIGIN='$lab_origin'
EOF

env PGOPTIONS='-c lock_timeout=5000 -c statement_timeout=30000' \
    psql -X --no-password -h "$PGHOST" -p "$PGPORT" -U "$DB_USER" -d "$PGDATABASE" \
    -v ON_ERROR_STOP=1 -f "$LAB_DIR/db_init.sql"
pause

lab_domain="$PAYARA_HOME/glassfish/domains/lab2"
if [ ! -f "$lab_domain/config/domain.xml" ]; then
    small_java "$PAYARA_HOME/bin/asadmin" create-domain --portbase "$lab_base" --nopassword=true lab2
    pause
fi
lab_xml="$lab_domain/config/domain.xml"
test -f "$lab_xml.setup-backup" || cp -p "$lab_xml" "$lab_xml.setup-backup"
cp -p "$lab_xml" "$lab_xml.before-app-reset"

awk -v base="$lab_base" '
    /<application-ref / && /ref="(client|organizations)"/ {
        if (! /<\/application-ref>/ && ! /\/>/) skip_ref=1
        next
    }
    skip_ref {if (/<\/application-ref>/) skip_ref=0; next}
    /<application / && /name="(client|organizations)"/ {
        if (! /<\/application>/ && ! /\/>/) skip_app=1
        next
    }
    skip_app {if (/<\/application>/) skip_app=0; next}
    /<jvm-options>-(Xms|Xmx|Xss)/ {next}
    /<jvm-options>-XX:(ActiveProcessorCount=|ReservedCodeCacheSize=|[+-]Use(G1|Parallel|Serial|Z|Shenandoah)GC)/ {next}
    /<\/java-config>/ {
        print "<jvm-options>-Xms32m</jvm-options>"
        print "<jvm-options>-Xmx192m</jvm-options>"
        print "<jvm-options>-Xss512k</jvm-options>"
        print "<jvm-options>-XX:ActiveProcessorCount=2</jvm-options>"
        print "<jvm-options>-XX:+UseSerialGC</jvm-options>"
        print "<jvm-options>-XX:ReservedCodeCacheSize=48m</jvm-options>"
    }
    /name="http-listener-1"/ {gsub(/ enabled="[^"]*"/, "")}
    /<network-listener / {
        gsub(/ address="[^"]*"/, "")
        sub(/<network-listener /, "<network-listener address=\"127.0.0.1\" ")
        if (/name="http-listener-1"/) {p=base+80; sub(/<network-listener /, "<network-listener enabled=\"false\" ")}
        else if (/name="http-listener-2"/) p=base+81
        else if (/name="admin-listener"/) p=base+48
        else p=0
        if(p) sub(/port="[^"]*"/, "port=\"" p "\"")
    }
    /<iiop-listener / {
        p=base+37
        if(/id="SSL"/) p=base+38
        if(/id="SSL_MUTUALAUTH"/) p=base+39
        sub(/port="[^"]*"/, "port=\"" p "\"")
    }
    /<jmx-connector / {sub(/port="[^"]*"/, "port=\"" base+86 "\"")}
    /<jms-host / {sub(/port="[^"]*"/, "port=\"" base+76 "\"")}
    /<system-property .*name="JMS_PROVIDER_PORT"/ {sub(/value="[^"]*"/, "value=\"" base+76 "\"")}
    /<hazelcast-config-specific-configuration[ \/>]/ {
        gsub(/ enabled="[^"]*"/, "")
        sub(/<hazelcast-config-specific-configuration/, "<hazelcast-config-specific-configuration enabled=\"false\"")
    }
    /<hazelcast-runtime-configuration[ \/>]/ {gsub(/ enabled="[^"]*"/, "")}
    /<thread-pool / {
        gsub(/ (min|max)-thread-pool-size="[^"]*"/, "")
        sub(/<thread-pool /, "<thread-pool min-thread-pool-size=\"1\" max-thread-pool-size=\"8\" ")
    }
    /<protocol .*name="http-listener-2"/ {tls=1}
    tls && /<ssl[ >]/ {
        gsub(/ cert-nickname="[^"]*"/, "")
        sub(/<ssl/, "<ssl cert-nickname=\"organizations-lab\"")
    }
    /<\/protocol>/ {tls=0}
    {gsub(/address="0.0.0.0"/, "address=\"127.0.0.1\""); print}
' "$lab_xml" > "$lab_xml.tmp"
mv "$lab_xml.tmp" "$lab_xml"
rm -f "$lab_domain/config/pid"

if ! small_java "$JAVA_HOME/bin/keytool" -list -alias organizations-lab -keystore "$lab_domain/config/keystore.p12" -storepass changeit >/dev/null 2>&1; then
    small_java "$JAVA_HOME/bin/keytool" -genkeypair -alias organizations-lab -keyalg RSA -keysize 2048 -validity 365 \
        -dname CN=localhost -ext SAN=dns:localhost,ip:127.0.0.1 \
        -keystore "$lab_domain/config/keystore.p12" -storepass changeit -keypass changeit
    pause
fi
small_java "$JAVA_HOME/bin/keytool" -exportcert -rfc -alias organizations-lab \
    -keystore "$lab_domain/config/keystore.p12" -storepass changeit -file "$LAB_DIR/organizations-cert.pem"
pause

lab_wf_config="$WILDFLY_HOME/standalone/configuration"
test -f "$lab_wf_config/standalone-lab.xml" || cp "$lab_wf_config/standalone.xml" "$lab_wf_config/standalone-lab.xml"
if [ ! -f "$lab_wf_config/people.p12" ]; then
    small_java "$JAVA_HOME/bin/keytool" -genkeypair -alias people-lab -keyalg RSA -keysize 2048 -validity 365 \
        -dname CN=localhost -ext SAN=dns:localhost,ip:127.0.0.1 -storetype PKCS12 \
        -keystore "$lab_wf_config/people.p12" -storepass changeit -keypass changeit
    pause
fi
small_java "$JAVA_HOME/bin/keytool" -exportcert -rfc -alias people-lab \
    -keystore "$lab_wf_config/people.p12" -storepass changeit -file "$LAB_DIR/people-cert.pem"
cp "$LAB_DIR/organizations-cert.pem" "$lab_wf_config/organizations-cert.pem"
pause

cat > "$LAB_DIR/wildfly-setup.cli" <<EOF
embed-server --server-config=standalone-lab.xml --std-out=echo
if (outcome != success) of /subsystem=elytron/key-store=labKS:read-resource
    /subsystem=elytron/key-store=labKS:add(path=people.p12,relative-to=jboss.server.config.dir,type=PKCS12,credential-reference={clear-text=changeit})
end-if
if (outcome != success) of /subsystem=elytron/key-manager=labKM:read-resource
    /subsystem=elytron/key-manager=labKM:add(key-store=labKS,alias-filter=people-lab,credential-reference={clear-text=changeit})
end-if
if (outcome != success) of /subsystem=elytron/server-ssl-context=labSSC:read-resource
    /subsystem=elytron/server-ssl-context=labSSC:add(key-manager=labKM)
end-if
/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=ssl-context,value=labSSC)
/subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=connector-ref,value=https)
if (outcome == success) of /subsystem=undertow/server=default-server/http-listener=default:read-resource
    /subsystem=undertow/server=default-server/http-listener=default:remove
end-if
/subsystem=io/worker=default:write-attribute(name=io-threads,value=1)
/subsystem=io/worker=default:write-attribute(name=task-core-threads,value=2)
/subsystem=io/worker=default:write-attribute(name=task-max-threads,value=8)
if (outcome != success) of /system-property=organizations.url:read-resource
    /system-property=organizations.url:add(value="$lab_org_url")
else
    /system-property=organizations.url:write-attribute(name=value,value="$lab_org_url")
end-if
if (outcome != success) of /system-property=organizations.caCert:read-resource
    /system-property=organizations.caCert:add(value="$lab_wf_config/organizations-cert.pem")
else
    /system-property=organizations.caCert:write-attribute(name=value,value="$lab_wf_config/organizations-cert.pem")
end-if
if (outcome != success) of /system-property=client.origin:read-resource
    /system-property=client.origin:add(value="$lab_origin")
else
    /system-property=client.origin:write-attribute(name=value,value="$lab_origin")
end-if
/subsystem=deployment-scanner/scanner=default:write-attribute(name=scan-enabled,value=true)
/subsystem=deployment-scanner/scanner=default:write-attribute(name=scan-interval,value=3000)
if (outcome == success) of /deployment=orgmanager.war:read-resource
    /deployment=orgmanager.war:remove
end-if
stop-embedded-server
EOF
phase 'Configuring WildFly'
small_java "$WILDFLY_HOME/bin/jboss-cli.sh" --file="$LAB_DIR/wildfly-setup.cli"
pause

lab_client_dir=$(mktemp -d "$LAB_DIR/client.XXXXXX")
unzip -q "$LAB_DIR/client.war" -d "$lab_client_dir"
if grep -q 'config.js' "$lab_client_dir/index.html"; then
    printf 'window.LAB_API = {org: "/organizations-api", people: "%s"};\n' "$lab_people_url" > "$lab_client_dir/config.js"
else
    for lab_asset in "$lab_client_dir"/assets/*.js; do
        test -f "$lab_asset" || continue
        sed -i '' "s@https://localhost:38443/orgmanager@$lab_people_url@g" "$lab_asset"
        sed -i '' 's@===`org`?``:@===`org`?`/organizations-api`:@g' "$lab_asset"
    done
    sed -i '' "s@\.js@.js?ports=$lab_https-$lab_people_https-api2@g" "$lab_client_dir/index.html"
fi
small_java "$JAVA_HOME/bin/jar" --create --file "$LAB_DIR/client-runtime.war" -C "$lab_client_dir" .
rm -rf "$lab_client_dir"
pause

phase 'Starting Payara'
"$PAYARA_HOME/bin/asadmin" start-domain lab2
pause
small_java "$PAYARA_HOME/bin/asadmin" --port "$lab_admin" deploy --force=true --name client --contextroot /client "$LAB_DIR/client-runtime.war"
pause
small_java "$PAYARA_HOME/bin/asadmin" --port "$lab_admin" deploy --force=true --name organizations --contextroot /organizations-api "$LAB_DIR/organizations.war"
pause

phase 'Preparing people deployment'
lab_deploy_dir="$WILDFLY_HOME/standalone/deployments"
mkdir -p "$lab_deploy_dir"
rm -f "$lab_deploy_dir/orgmanager.war.deployed" "$lab_deploy_dir/orgmanager.war.failed" \
    "$lab_deploy_dir/orgmanager.war.undeployed" "$lab_deploy_dir/orgmanager.war.pending"
cp "$LAB_DIR/orgmanager.war" "$lab_deploy_dir/orgmanager.war.tmp"
mv "$lab_deploy_dir/orgmanager.war.tmp" "$lab_deploy_dir/orgmanager.war"
touch "$lab_deploy_dir/orgmanager.war.dodeploy"
pause
phase 'Starting WildFly'
nohup "$WILDFLY_HOME/bin/standalone.sh" -c standalone-lab.xml -b 127.0.0.1 -bmanagement 127.0.0.1 \
    -Djboss.socket.binding.port-offset="$lab_offset" > "$LAB_DIR/wildfly.log" 2>&1 < /dev/null &
lab_wf_pid=$!
lab_wait=0
until grep -q WFLYSRV0025 "$LAB_DIR/wildfly.log"; do
    kill -0 "$lab_wf_pid" 2>/dev/null || fail 'WildFly exited during startup'
    lab_wait=$((lab_wait+1))
    test "$lab_wait" -lt 40 || fail 'WildFly did not start within 120 seconds'
    pause
done
pause
phase 'Waiting for orgmanager.war deployment'
lab_wait=0
until [ -f "$lab_deploy_dir/orgmanager.war.deployed" ]; do
    if [ -f "$lab_deploy_dir/orgmanager.war.failed" ]; then
        cat "$lab_deploy_dir/orgmanager.war.failed" >&2
        fail 'People deployment failed'
    fi
    kill -0 "$lab_wf_pid" 2>/dev/null || fail 'WildFly exited during deployment'
    lab_wait=$((lab_wait+1))
    test "$lab_wait" -lt 40 || fail 'People deployment did not complete within 120 seconds'
    pause
done
pause
phase 'Checking HTTPS endpoints'

curl --fail --silent --show-error --max-time 45 --cacert "$LAB_DIR/organizations-cert.pem" \
    -H 'Content-Type: application/xml' -d '<organizationListRequest/>' "$lab_org_url/organizations/search" >/dev/null
pause
curl --fail --silent --show-error --max-time 45 --cacert "$LAB_DIR/people-cert.pem" "$lab_people_url/persons" >/dev/null
pause
curl --fail --silent --show-error --max-time 45 --cacert "$LAB_DIR/organizations-cert.pem" "$lab_origin/client/" >/dev/null

printf '\nREADY. Open a tunnel on your Mac:\n'
printf 'ssh -p 2222 -N -L 127.0.0.1:%s:127.0.0.1:%s -L 127.0.0.1:%s:127.0.0.1:%s s414257@se.ifmo.ru\n' \
    "$lab_https" "$lab_https" "$lab_people_https" "$lab_people_https"
printf '\nFirst accept the certificate at %s/persons\nThen open %s/client/\n' "$lab_people_url" "$lab_origin"
