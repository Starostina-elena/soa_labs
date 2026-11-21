import { useEffect, useState } from 'react';
import { api, personXml, readPerson } from '../api/index.js';
import PersonEditor from './PersonEditor.jsx';

export default function People({ act, busy }) {
    const [items, setItems] = useState([]);
    const [editing, setEditing] = useState(null);
    const [name, setName] = useState('');
    const [ids, setIds] = useState('');
    const [fireId, setFireId] = useState('');
    async function load() {
        const doc = await api('people', '/persons');
        setItems([...doc.querySelectorAll('items > person')].map(readPerson));
    }
    useEffect(() => {
        act(load);
    }, []);
    function reset() {
        setName('');
        setIds('');
    }
    async function open(id) {
        const doc = await api('people', `/persons/${encodeURIComponent(id)}`);
        setEditing(readPerson(doc.documentElement));
    }

    if (editing)
        return (
            <PersonEditor
                person={editing}
                act={act}
                busy={busy}
                onSave={async () => {
                    await load();
                    setEditing(null);
                }}
                onCancel={() => setEditing(null)}
            />
        );
    return (
        <>
            <section>
                <h2>Сотрудники</h2>
                <button disabled={busy} onClick={() => act(load)}>
                    Обновить список
                </button>
                <div className="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Имя</th>
                                <th>Организации</th>
                                <th>Действия</th>
                            </tr>
                        </thead>
                        <tbody>
                            {items.map((p) => (
                                <tr key={p.id}>
                                    <td>{p.id}</td>
                                    <td>{p.name}</td>
                                    <td>
                                        {p.organizations
                                            .map((o) => `${o.name} (ID ${o.id})`)
                                            .join(', ') || 'Не работает в организациях'}
                                    </td>
                                    <td>
                                        <button
                                            disabled={busy}
                                            onClick={() => act(() => open(p.id))}
                                        >
                                            Изменить
                                        </button>
                                        <button
                                            disabled={busy}
                                            className="secondary"
                                            onClick={() =>
                                                act(async () => {
                                                    await api(
                                                        'people',
                                                        `/persons/${p.id}`,
                                                        'DELETE',
                                                    );
                                                    await load();
                                                }, 'Сотрудник удалён.')
                                            }
                                        >
                                            Удалить
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {!items.length && (
                                <tr>
                                    <td colSpan="4">Сотрудников нет</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </section>
            <section>
                <h2>Новый сотрудник</h2>
                <form
                    onSubmit={(e) => {
                        e.preventDefault();
                        act(async () => {
                            await api('people', '/persons', 'POST', personXml(name, ids));
                            reset();
                            await load();
                        }, 'Сотрудник создан.');
                    }}
                >
                    <fieldset disabled={busy}>
                        <div className="fields">
                            <label>
                                Имя
                                <input
                                    required
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                />
                            </label>
                            <label>
                                ID организаций через запятую
                                <input
                                    value={ids}
                                    placeholder="1, 2, 3"
                                    onChange={(e) => setIds(e.target.value)}
                                />
                            </label>
                        </div>
                        <button>Создать сотрудника</button>
                        <button type="button" className="secondary" onClick={reset}>
                            Очистить
                        </button>
                    </fieldset>
                </form>
            </section>
            <section>
                <h2>Уволить всех из организации</h2>
                <p>Сотрудники останутся в списке. Их работа в других организациях сохранится.</p>
                <form
                    className="row"
                    onSubmit={(e) => {
                        e.preventDefault();
                        act(async () => {
                            await api('people', `/fire/all/${encodeURIComponent(fireId)}`, 'POST');
                            await load();
                        }, 'Сотрудники уволены из организации.');
                    }}
                >
                    <label>
                        ID организации для увольнения
                        <input
                            required
                            value={fireId}
                            onChange={(e) => setFireId(e.target.value)}
                        />
                    </label>
                    <button disabled={busy}>Уволить всех</button>
                </form>
            </section>
        </>
    );
}
