import { useState } from 'react';
import { api, personXml } from '../api/index.js';

export default function PersonEditor({ person, act, busy, onSave, onCancel }) {
    const [name, setName] = useState(person.name);
    const [ids, setIds] = useState(person.organizationIds.join(', '));
    return (
        <section>
            <h2>Изменить сотрудника №{person.id}</h2>
            <p>ID: {person.id}</p>
            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    act(async () => {
                        await api('people', `/persons/${person.id}`, 'PATCH', personXml(name, ids));
                        await onSave();
                    }, 'Сотрудник изменён.');
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
                    <button>Сохранить сотрудника</button>
                    <button type="button" className="secondary" onClick={onCancel}>
                        Назад к сотрудникам
                    </button>
                </fieldset>
            </form>
        </section>
    );
}
