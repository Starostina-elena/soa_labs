import { types, typeNames } from '../api/index.js';

export default function OrganizationFields({ value, onChange }) {
    function field(key, label, props = {}) {
        return (
            <label>
                {label}
                <input
                    value={value[key]}
                    onChange={(e) => onChange({ ...value, [key]: e.target.value })}
                    {...props}
                />
            </label>
        );
    }
    return (
        <div className="fields">
            {field('name', 'Название', { required: true })}
            {field('x', 'Координата X', { required: true, type: 'number', max: '775', step: '1' })}
            {field('y', 'Координата Y', { required: true, type: 'number', step: '1' })}
            {field('annualTurnover', 'Годовой оборот', {
                required: true,
                type: 'number',
                min: '0',
                step: 'any',
            })}
            {field('fullName', 'Полное название')}
            <label>
                Тип
                <select
                    value={value.type}
                    onChange={(e) => onChange({ ...value, type: e.target.value })}
                >
                    <option value="">Не указан</option>
                    {types.map((type) => (
                        <option key={type} value={type}>
                            {typeNames[type]}
                        </option>
                    ))}
                </select>
            </label>
            {field('street', 'Улица', { required: true })}
            {field('zipCode', 'Почтовый индекс')}
        </div>
    );
}
