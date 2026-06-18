let fontSize = 12;
if (/Mobi|Android/i.test(navigator.userAgent)) {
    fontSize = 7;
}
function fetchCredentials() {
    fetch('/list_credentials_json')
        .then(response => response.json())
        .then(data => {
            const container = document.getElementById('credentials-table');
            container.innerHTML = '';
            data.forEach(fileData => {
                const title = document.createElement('h2');
                title.textContent = fileData.name;
                container.appendChild(title);
                const table = document.createElement('table');
                table.className = 'styled-table';
                const thead = document.createElement('thead');
                const headerRow = document.createElement('tr');
                fileData.headers.forEach(h => {
                    const th = document.createElement('th');
                    th.textContent = h;
                    headerRow.appendChild(th);
                });
                thead.appendChild(headerRow);
                table.appendChild(thead);
                const tbody = document.createElement('tbody');
                fileData.rows.forEach(row => {
                    const tr = document.createElement('tr');
                    row.forEach(cell => {
                        const td = document.createElement('td');
                        td.textContent = cell;
                        tr.appendChild(td);
                    });
                    tbody.appendChild(tr);
                });
                table.appendChild(tbody);
                container.appendChild(table);
            });
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

document.addEventListener("DOMContentLoaded", function() {
    fetchCredentials();
    setInterval(fetchCredentials, 20000);
});
function adjustCredFontSize(change) {
    fontSize += change;
    document.getElementById('credentials-table').style.fontSize = fontSize + 'px';
}

function toggleCredToolbar() {
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
