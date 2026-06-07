let fontSize = 12;
if (/Mobi|Android/i.test(navigator.userAgent)) {
    fontSize = 7;
}

function fetchNetworkData() {
    fetch('/network_data_json')
        .then(response => response.json())
        .then(data => {
            const container = document.getElementById('network-table');
            container.innerHTML = '';
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
                row.forEach(cell => {
                    const td = document.createElement('td');
                    td.textContent = cell;
                    td.className = cell.trim() ? 'green' : 'red';
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

function adjustNetworkFontSize(change) {
    fontSize += change;
    document.getElementById('network-table').style.fontSize = fontSize + 'px';
}

function toggleNetworkToolbar() {
    const mainToolbar = document.querySelector('.toolbar');
    const toggleButton = document.getElementById('toggle-toolbar');
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
    fetchNetworkData();
    setInterval(fetchNetworkData, 60000);
});
