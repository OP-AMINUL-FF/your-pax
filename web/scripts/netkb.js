let fontSize = 12;
if (/Mobi|Android/i.test(navigator.userAgent)) {
    fontSize = 7;
}
function fetchNetkbData() {
    fetch('/netkb_data_json_full')
        .then(response => response.json())
        .then(data => {
            const container = document.getElementById('netkb-table');
            container.innerHTML = '';
            if (!data.headers || data.headers.length === 0) return;
            const table = document.createElement('table');
            table.className = 'styled-table';
            const thead = document.createElement('thead');
            const headerRow = document.createElement('tr');
            data.headers.forEach(h => {
                const th = document.createElement('th');
                th.textContent = h;
                headerRow.appendChild(th);
            });
            thead.appendChild(headerRow);
            table.appendChild(thead);
            const tbody = document.createElement('tbody');
            data.rows.forEach(row => {
                const tr = document.createElement('tr');
                if (row[3] === '0') tr.className = 'blue-row';
                row.forEach(cell => {
                    const td = document.createElement('td');
                    td.textContent = cell;
                    if (cell.includes('success')) td.className = 'green bold';
                    else if (cell.includes('failed')) td.className = 'red bold';
                    else if (cell.trim() === '') td.className = 'grey';
                    tr.appendChild(td);
                });
                tbody.appendChild(tr);
            });
            table.appendChild(tbody);
            container.appendChild(table);
        })
        .catch(error => {
            console.error('Error:', error);
        });
}
function adjustNetkbFontSize(change) {
    fontSize += change;
    document.getElementById('netkb-table').style.fontSize = fontSize + 'px';
}

function toggleNetkbToolbar() {
    const mainToolbar = document.querySelector('.toolbar');
    const toggleButton = document.getElementById('toggle-toolbar')
    const toggleIcon = document.getElementById('toggle-icon');
    if (mainToolbar.classList.contains('hidden')) {
        mainToolbar.classList.remove('hidden');
        toggleIcon.src = '/web/images/hide.png';
        toggleButton.setAttribute('data-open', 'true');
    } else {
        mainToolbar.classList.add('hidden');
        toggleIcon.src = '/web/images/reveal.png';
        toggleButton.setAttribute('data-open', 'false');

    }
}

document.addEventListener("DOMContentLoaded", function() {
    fetchNetkbData();
    setInterval(fetchNetkbData, 10000);
});
