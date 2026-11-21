export const types = ['PUBLIC', 'TRUST', 'OPEN_JOINT_STOCK_COMPANY'];

export const typeNames = {
    PUBLIC: 'Публичная',
    TRUST: 'Траст',
    OPEN_JOINT_STOCK_COMPANY: 'Открытое акционерное общество',
};

export const fields = [
    ['id', 'ID', 'number'],
    ['name', 'Название', 'string'],
    ['coordinates.x', 'Координата X', 'number'],
    ['coordinates.y', 'Координата Y', 'number'],
    ['creationDate', 'Дата создания', 'date'],
    ['annualTurnover', 'Годовой оборот', 'number'],
    ['fullName', 'Полное название', 'string'],
    ['type', 'Тип', 'type'],
    ['postalAddress.street', 'Улица', 'string'],
    ['postalAddress.zipCode', 'Индекс', 'string'],
];

export const emptyOrganization = {
    name: '',
    x: '',
    y: '',
    annualTurnover: '',
    fullName: '',
    type: '',
    street: '',
    zipCode: '',
};

const escape = (value) =>
    String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&apos;');

export const tag = (name, value) => `<${name}>${escape(value)}</${name}>`;

const wrap = (name, xml) => `<${name}>${xml}</${name}>`;

export const text = (element, name) => element?.querySelector(name)?.textContent ?? '';

export async function api(service, path, method = 'GET', body) {
    let response;
    try {
        const base =
            window.LAB_API?.[service] ??
            (service === 'org'
                ? (import.meta.env.VITE_ORG_API_URL ?? '/api/org')
                : (import.meta.env.VITE_PEOPLE_API_URL ?? '/api/people'));
        response = await fetch(`${base}${path}`, {
            method,
            headers: {
                Accept: 'application/xml',
                ...(body !== undefined ? { 'Content-Type': 'application/xml' } : {}),
            },
            body,
            signal: AbortSignal.timeout(45000),
        });
    } catch {
        throw new Error(
            'Не удалось дождаться ответа. Проверьте соединение. Перед повтором создания обновите список: запрос мог успеть выполниться.',
        );
    }
    const bodyText = await response.text();
    const document = bodyText ? new DOMParser().parseFromString(bodyText, 'application/xml') : null;
    if (!response.ok) {
        const message =
            document && !document.querySelector('parsererror') ? text(document, 'message') : '';
        const details = document
            ? [...document.querySelectorAll('errorDetail')].map(
                  (e) => `${text(e, 'parameter')}: ${text(e, 'message')}`,
              )
            : [];
        const error = new Error(message || `Ошибка сервиса: HTTP ${response.status}`);
        error.details = details;
        throw error;
    }
    if (document?.querySelector('parsererror')) throw new Error('Сервис вернул некорректный XML.');
    return document;
}

export function organizationXml(o) {
    return wrap(
        'organization',
        tag('name', o.name) +
            wrap('coordinates', tag('x', o.x) + tag('y', o.y)) +
            tag('annualTurnover', o.annualTurnover) +
            tag('fullName', o.fullName) +
            (o.type ? tag('type', o.type) : '') +
            wrap('postalAddress', tag('street', o.street) + tag('zipCode', o.zipCode)),
    );
}

export function readOrganization(e) {
    return {
        id: text(e, 'id'),
        name: text(e, 'name'),
        x: text(e, 'coordinates > x'),
        y: text(e, 'coordinates > y'),
        annualTurnover: text(e, 'annualTurnover'),
        fullName: text(e, 'fullName'),
        type: text(e, 'type'),
        street: text(e, 'postalAddress > street'),
        zipCode: text(e, 'postalAddress > zipCode'),
        creationDate: text(e, 'creationDate'),
    };
}

export function personXml(name, organizationIds) {
    const ids = organizationIds
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
    return wrap(
        'person',
        tag('name', name) +
            wrap('organizationIds', ids.map((id) => tag('organizationId', id)).join('')),
    );
}

export function readPerson(e) {
    return {
        id: text(e, ':scope > id'),
        name: text(e, ':scope > name'),
        organizationIds: [...e.querySelectorAll('organizationIds > *')].map((x) => x.textContent),
        organizations: [...e.querySelectorAll('organizations > organization')].map((o) => ({
            id: text(o, 'id'),
            name: text(o, 'name'),
        })),
    };
}

export function searchXml(page, size, sorting, filters) {
    const tree = {};
    for (const filter of filters) {
        if (!filter.value.trim())
            throw new Error('Заполните значение каждого фильтра или удалите пустую строку.');
        const path = filter.field.split('.');
        let node = tree;
        for (const part of path) node = node[part] ??= {};
        if (node[filter.op] !== undefined)
            throw new Error(`Фильтр ${filter.field} с условием ${filter.op} уже задан.`);
        node[filter.op] =
            filter.op === 'in'
                ? wrap(
                      'in',
                      filter.value
                          .split(',')
                          .map((s) => s.trim())
                          .filter(Boolean)
                          .map((type) => tag('type', type))
                          .join(''),
                  )
                : tag(filter.op, filter.value);
    }
    function xml(node) {
        return Object.entries(node)
            .map(([key, value]) => (typeof value === 'string' ? value : wrap(key, xml(value))))
            .join('');
    }
    return wrap(
        'organizationListRequest',
        tag('page', page) +
            tag('size', size) +
            wrap(
                'sort',
                sorting
                    .filter((s) => s.field)
                    .map((s) => tag('string', (s.desc ? '-' : '') + s.field))
                    .join(''),
            ) +
            wrap('filters', xml(tree)),
    );
}
