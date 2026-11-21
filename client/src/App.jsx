import { useState } from 'react';
import Organizations from './organizations/Organizations.jsx';
import People from './people/People.jsx';

export default function App() {
    const [tab, setTab] = useState('organizations');
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState(null);
    async function act(action, success = '') {
        setBusy(true);
        setMessage('');
        setError(null);
        try {
            await action();
            setMessage(success);
        } catch (e) {
            setError(e);
        } finally {
            setBusy(false);
        }
    }
    function changeTab(value) {
        setTab(value);
        setMessage('');
        setError(null);
    }
    return (
        <main>
            <h1>Организации и сотрудники</h1>
            <nav aria-label="Разделы">
                <button
                    disabled={busy}
                    className={tab === 'organizations' ? '' : 'secondary'}
                    onClick={() => changeTab('organizations')}
                >
                    Организации
                </button>
                <button
                    disabled={busy}
                    className={tab === 'people' ? '' : 'secondary'}
                    onClick={() => changeTab('people')}
                >
                    Сотрудники
                </button>
            </nav>
            {busy && <p role="status">Загрузка…</p>}
            {message && (
                <p className="success" role="status">
                    {message}
                </p>
            )}
            {error && (
                <div className="error" role="alert">
                    <p>{error.message}</p>
                    {error.details?.length > 0 && (
                        <ul>
                            {error.details.map((d, i) => (
                                <li key={i}>{d}</li>
                            ))}
                        </ul>
                    )}
                </div>
            )}
            {tab === 'organizations' && <Organizations act={act} busy={busy} />}
            {tab === 'people' && <People act={act} busy={busy} />}
        </main>
    );
}
