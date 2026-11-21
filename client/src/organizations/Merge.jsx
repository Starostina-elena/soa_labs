import { useState } from 'react';
import { api, emptyOrganization, text, organizationXml } from '../api/index.js';
import OrganizationFields from './OrganizationFields.jsx';

export default function Merge({ act, busy, onMerge }) {
    const [first, setFirst] = useState('');
    const [second, setSecond] = useState('');
    const [form, setForm] = useState({ ...emptyOrganization });
    const [result, setResult] = useState(null);
    return (
        <div>
            <h3>Объединить организации</h3>
            <p>
                Сотрудники перейдут в новую организацию. Обе исходные организации будут удалены.
                Другие места работы сохранятся.
            </p>
            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    act(async () => {
                        setResult(null);
                        const doc = await api(
                            'people',
                            `/merge/${encodeURIComponent(first)}/${encodeURIComponent(second)}/new-name/new-address`,
                            'POST',
                            organizationXml(form),
                        );
                        setResult({ id: text(doc, 'id'), name: text(doc, 'name') });
                        await onMerge();
                    }, 'Организации объединены.');
                }}
            >
                <fieldset disabled={busy}>
                    <div className="fields">
                        <label>
                            ID первой организации
                            <input
                                required
                                value={first}
                                onChange={(e) => setFirst(e.target.value)}
                            />
                        </label>
                        <label>
                            ID второй организации
                            <input
                                required
                                value={second}
                                onChange={(e) => setSecond(e.target.value)}
                            />
                        </label>
                    </div>
                    <h3>Данные новой организации</h3>
                    <OrganizationFields value={form} onChange={setForm} />
                    <button>Объединить и удалить исходные</button>
                </fieldset>
            </form>
            {result && (
                <p role="status">
                    Создана организация «{result.name}», ID: {result.id}.
                </p>
            )}
        </div>
    );
}
