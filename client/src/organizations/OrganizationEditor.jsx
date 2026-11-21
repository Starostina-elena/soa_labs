import { useState } from 'react';
import { api, organizationXml } from '../api/index.js';
import OrganizationFields from './OrganizationFields.jsx';

export default function OrganizationEditor({ organization, act, busy, onSave, onCancel }) {
    const [form, setForm] = useState({ ...organization });
    return (
        <section>
            <h2>Изменить организацию №{organization.id}</h2>
            <p>ID: {organization.id}</p>
            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    act(async () => {
                        await api(
                            'org',
                            `/organizations/${organization.id}`,
                            'PATCH',
                            organizationXml(form),
                        );
                        await onSave();
                    }, 'Организация изменена.');
                }}
            >
                <fieldset disabled={busy}>
                    <OrganizationFields value={form} onChange={setForm} />
                    <button>Сохранить организацию</button>
                    <button type="button" className="secondary" onClick={onCancel}>
                        Назад к организациям
                    </button>
                </fieldset>
            </form>
        </section>
    );
}
