import { useEffect, useState } from 'react';
import {
    api,
    types,
    typeNames,
    fields,
    emptyOrganization,
    text,
    organizationXml,
    readOrganization,
    searchXml,
} from '../api/index.js';
import OrganizationFields from './OrganizationFields.jsx';
import OrganizationEditor from './OrganizationEditor.jsx';
import Merge from './Merge.jsx';

export default function Organizations({ act, busy }) {
    const [items, setItems] = useState([]);
    const [page, setPage] = useState(1);
    const [size, setSize] = useState('10');
    const [total, setTotal] = useState(0);
    const [applied, setApplied] = useState({ size: '10', sorting: [], filters: [] });
    const [filters, setFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [form, setForm] = useState({ ...emptyOrganization });
    const [editing, setEditing] = useState(null);
    const [deleteType, setDeleteType] = useState('PUBLIC');
    const [turnover, setTurnover] = useState('100');
    const [count, setCount] = useState(null);

    async function load(number = page, criteria = applied) {
        const doc = await api(
            'org',
            '/organizations/search',
            'POST',
            searchXml(number, criteria.size, criteria.sorting, criteria.filters),
        );
        setItems([...doc.querySelectorAll('items > organization')].map(readOrganization));
        setTotal(Number(text(doc, 'total')));
        setPage(number);
        setApplied(criteria);
    }
    useEffect(() => {
        act(() => load(1));
    }, []);
    async function open(id) {
        const doc = await api('org', `/organizations/${encodeURIComponent(id)}`);
        const organization = readOrganization(doc.documentElement);
        setEditing(organization);
    }
    function reset() {
        setForm({ ...emptyOrganization });
    }
    function changeFilter(index, patch) {
        setFilters(filters.map((f, i) => (i === index ? { ...f, ...patch } : f)));
    }
    function changeSort(index, patch) {
        setSorting(sorting.map((s, i) => (i === index ? { ...s, ...patch } : s)));
    }

    if (editing)
        return (
            <OrganizationEditor
                organization={editing}
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
                <h2>Организации</h2>
                <form
                    onSubmit={(e) => {
                        e.preventDefault();
                        act(() => load(1, { size, sorting, filters }));
                    }}
                >
                    <fieldset disabled={busy}>
                        <legend>Поиск</legend>
                        <p className="hint">
                            Все фильтры применяются одновременно. Сортировка идёт сверху вниз.
                        </p>
                        {filters.map((filter, i) => {
                            const kind = fields.find((f) => f[0] === filter.field)[2];
                            const operators =
                                kind === 'string'
                                    ? [
                                          ['eq', '='],
                                          ['contains', 'содержит'],
                                      ]
                                    : kind === 'type'
                                      ? [
                                            ['eq', '='],
                                            ['in', 'в списке'],
                                        ]
                                      : [
                                            ['eq', '='],
                                            ['gt', '>'],
                                            ['gte', '>='],
                                            ['lt', '<'],
                                            ['lte', '<='],
                                        ];
                            return (
                                <div className="row" key={i}>
                                    <label>
                                        Поле фильтра
                                        <select
                                            value={filter.field}
                                            onChange={(e) =>
                                                changeFilter(i, {
                                                    field: e.target.value,
                                                    op: 'eq',
                                                    value: '',
                                                })
                                            }
                                        >
                                            {fields.map((f) => (
                                                <option key={f[0]} value={f[0]}>
                                                    {f[1]}
                                                </option>
                                            ))}
                                        </select>
                                    </label>
                                    <label>
                                        Условие
                                        <select
                                            value={filter.op}
                                            onChange={(e) =>
                                                changeFilter(i, { op: e.target.value, value: '' })
                                            }
                                        >
                                            {operators.map((o) => (
                                                <option key={o[0]} value={o[0]}>
                                                    {o[1]}
                                                </option>
                                            ))}
                                        </select>
                                    </label>
                                    <label>
                                        Значение
                                        {kind === 'type' && filter.op === 'eq' ? (
                                            <select
                                                required
                                                value={filter.value}
                                                onChange={(e) =>
                                                    changeFilter(i, { value: e.target.value })
                                                }
                                            >
                                                <option value="">Выберите тип</option>
                                                {types.map((t) => (
                                                    <option key={t} value={t}>
                                                        {typeNames[t]}
                                                    </option>
                                                ))}
                                            </select>
                                        ) : (
                                            <input
                                                required
                                                value={filter.value}
                                                onChange={(e) =>
                                                    changeFilter(i, { value: e.target.value })
                                                }
                                                placeholder={
                                                    kind === 'date'
                                                        ? '2026-01-01T00:00:00Z'
                                                        : filter.op === 'in'
                                                          ? 'PUBLIC, TRUST'
                                                          : ''
                                                }
                                            />
                                        )}
                                    </label>
                                    <button
                                        type="button"
                                        className="secondary"
                                        onClick={() =>
                                            setFilters(filters.filter((_, index) => index !== i))
                                        }
                                    >
                                        Убрать фильтр
                                    </button>
                                </div>
                            );
                        })}
                        <button
                            type="button"
                            className="secondary"
                            onClick={() =>
                                setFilters([
                                    ...filters,
                                    { field: 'name', op: 'contains', value: '' },
                                ])
                            }
                        >
                            Добавить фильтр
                        </button>
                        {sorting.map((sort, i) => (
                            <div className="row" key={i}>
                                <label>
                                    Поле сортировки
                                    <select
                                        value={sort.field}
                                        onChange={(e) => changeSort(i, { field: e.target.value })}
                                    >
                                        {fields.map((f) => (
                                            <option key={f[0]} value={f[0]}>
                                                {f[1]}
                                            </option>
                                        ))}
                                    </select>
                                </label>
                                <label>
                                    Порядок
                                    <select
                                        value={sort.desc ? 'desc' : 'asc'}
                                        onChange={(e) =>
                                            changeSort(i, { desc: e.target.value === 'desc' })
                                        }
                                    >
                                        <option value="asc">По возрастанию</option>
                                        <option value="desc">По убыванию</option>
                                    </select>
                                </label>
                                <button
                                    type="button"
                                    className="secondary"
                                    onClick={() =>
                                        setSorting(sorting.filter((_, index) => index !== i))
                                    }
                                >
                                    Убрать сортировку
                                </button>
                            </div>
                        ))}
                        <button
                            type="button"
                            className="secondary"
                            onClick={() => setSorting([...sorting, { field: 'id', desc: false }])}
                        >
                            Добавить сортировку
                        </button>
                        <div className="row">
                            <label>
                                На странице
                                <input
                                    type="number"
                                    min="1"
                                    max="2147483647"
                                    required
                                    value={size}
                                    onChange={(e) => setSize(e.target.value)}
                                />
                            </label>
                            <button type="submit">Показать</button>
                            <button
                                type="button"
                                className="secondary"
                                onClick={() => {
                                    setFilters([]);
                                    setSorting([]);
                                }}
                            >
                                Очистить условия
                            </button>
                        </div>
                    </fieldset>
                </form>
                <p>Найдено: {total}</p>
                <div className="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Название</th>
                                <th>X</th>
                                <th>Y</th>
                                <th>Дата создания</th>
                                <th>Оборот</th>
                                <th>Полное название</th>
                                <th>Тип</th>
                                <th>Улица</th>
                                <th>Индекс</th>
                                <th>Действия</th>
                            </tr>
                        </thead>
                        <tbody>
                            {items.map((o) => (
                                <tr key={o.id}>
                                    <td>{o.id}</td>
                                    <td>{o.name}</td>
                                    <td>{o.x}</td>
                                    <td>{o.y}</td>
                                    <td>{new Date(o.creationDate).toLocaleString('ru-RU')}</td>
                                    <td>{o.annualTurnover}</td>
                                    <td>{o.fullName || '—'}</td>
                                    <td>{typeNames[o.type] || '—'}</td>
                                    <td>{o.street}</td>
                                    <td>{o.zipCode || '—'}</td>
                                    <td>
                                        <button
                                            disabled={busy}
                                            onClick={() => act(() => open(o.id))}
                                        >
                                            Изменить
                                        </button>
                                        <button
                                            disabled={busy}
                                            className="secondary"
                                            onClick={() =>
                                                act(async () => {
                                                    await api(
                                                        'org',
                                                        `/organizations/${o.id}`,
                                                        'DELETE',
                                                    );
                                                    await load();
                                                }, 'Организация удалена.')
                                            }
                                        >
                                            Удалить
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {!items.length && (
                                <tr>
                                    <td colSpan="11">Организаций нет</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
                <form
                    className="row"
                    onSubmit={(e) => {
                        e.preventDefault();
                        act(() => load(Number(page)));
                    }}
                >
                    <button
                        type="button"
                        disabled={busy || Number(page) <= 1}
                        onClick={() => act(() => load(Number(page) - 1))}
                    >
                        Назад
                    </button>
                    <label>
                        Страница
                        <input
                            type="number"
                            min="1"
                            max="2147483647"
                            required
                            value={page}
                            onChange={(e) => setPage(e.target.value)}
                        />
                    </label>
                    <button disabled={busy} type="submit">
                        Перейти
                    </button>
                    <button
                        type="button"
                        disabled={busy || Number(page) * Number(applied.size) >= total}
                        onClick={() => act(() => load(Number(page) + 1))}
                    >
                        Дальше
                    </button>
                </form>
            </section>
            <section>
                <h2>Новая организация</h2>
                <form
                    onSubmit={(e) => {
                        e.preventDefault();
                        act(async () => {
                            await api('org', '/organizations', 'POST', organizationXml(form));
                            reset();
                            await load(1);
                        }, 'Организация создана.');
                    }}
                >
                    <fieldset disabled={busy}>
                        <OrganizationFields value={form} onChange={setForm} />
                        <button>Создать организацию</button>
                        <button type="button" className="secondary" onClick={reset}>
                            Очистить
                        </button>
                    </fieldset>
                </form>
            </section>
            <section>
                <h2>Дополнительные операции</h2>
                <form
                    className="row"
                    onSubmit={(e) => {
                        e.preventDefault();
                        act(async () => {
                            const doc = await api(
                                'org',
                                `/organizations/count-annual-turnover?annualTurnover=${encodeURIComponent(turnover)}`,
                            );
                            setCount(text(doc, 'count'));
                        });
                    }}
                >
                    <label>
                        Оборот больше
                        <input
                            required
                            type="number"
                            min="0"
                            step="any"
                            value={turnover}
                            onChange={(e) => setTurnover(e.target.value)}
                        />
                    </label>
                    <button disabled={busy}>Посчитать</button>
                    {count !== null && <span role="status">Количество: {count}</span>}
                </form>
                <form
                    className="row"
                    onSubmit={(e) => {
                        e.preventDefault();
                        act(async () => {
                            await api('org', `/organizations/type/${deleteType}`, 'DELETE');
                            reset();
                            await load(1);
                        }, 'Одна организация этого типа удалена.');
                    }}
                >
                    <label>
                        Тип для удаления
                        <select value={deleteType} onChange={(e) => setDeleteType(e.target.value)}>
                            {types.map((t) => (
                                <option key={t} value={t}>
                                    {typeNames[t]}
                                </option>
                            ))}
                        </select>
                    </label>
                    <button disabled={busy} className="secondary">
                        Удалить одну организацию этого типа
                    </button>
                </form>
                <Merge
                    act={act}
                    busy={busy}
                    onMerge={async () => {
                        await load(1);
                        setCount(null);
                    }}
                />
            </section>
        </>
    );
}
