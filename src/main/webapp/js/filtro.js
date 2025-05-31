const filterButtons = document.querySelectorAll('.filter-btn');
    const products = document.querySelectorAll('.product');

    filterButtons.forEach(button => {
        button.addEventListener('click', () => {
            // Marca o botão ativo
            filterButtons.forEach(btn => btn.classList.remove('active'));
            button.classList.add('active');

            const filter = button.getAttribute('data-filter');

            // Mostra ou oculta produtos com base no filtro
            products.forEach(product => {
                const type = product.getAttribute('data-type');
                if (filter === 'todos' || filter === type) {
                    product.style.display = 'inline-block';
                } else {
                    product.style.display = 'none';
                }
            });
        });
    });
