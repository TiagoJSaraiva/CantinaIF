# CantinaIF
## Projeto Final de CJOWEB2 - IFSP-CJO 2025

  <p>Este projeto visa desenvolver uma aplicação web para auxiliar o acesso às cantinas de Institutos Federais (IFs), que muitas vezes se encontram congestionadas em horário de intervalo ou troca de aulas. Este projeto utiliza como principal ferramenta o acesso dos alunos aos seus celulares ou computadores durante a aula, pois assim eles podem acessar um site onde o responsável pela cantina coloca os salgados, doces, bebidas e lanches especiais disponíveis no dia. Com esse sistema, os alunos e colaboradores da instituição podem mandar um pedido informando ao vendedor que possuem interesse em comprar determinado item com antecedência.</p>
  <p>Um sistema como esse oferece várias vantagens. Um exemplo é que o vendedor pode começar a preparar os alimentos enquanto os potenciais compradores ainda estão em suas aulas, e isso ajuda o vendedor a entender quantos e quais produtos deixar prontos, e também permite que os compradores deixem seu lanche reservado ao pedirem com antecedência. A aplicação NÃO tem relação com delivery de pedidos!</p>

#### Funcionamento da aplicação
  - Os usuários devem fornecer seu prontuário institucional, CPF e nome para que o cadastro seja realizado com sucesso; os documentos como prontuário e CPF serão verificados com as informações da instituição relacionada para confirmar que seja realmente um aluno ou colaborador, pois esse é um sistema com escopo fechado, funcionando apenas dentro dos IFs e não tendo relação com clientes de fora. Nem mesmo o aluno de uma faculdade que usa esse sistema porá usar seu login para comprar em uma faculdade diferente da sua, uma vez que cada cantina terá seu sistema fechado para si e poderá ser acessada apenas pelos alunos e colaboradores de sua mesma unidade.<br><br>
    **De acordo com o prontuário usado para login, o sistema direciona o usuário para a cantina correspondente.*

  - Usuários do tipo **Comprador** têm acesso a uma página inicial que mostra os itens disponíveis na cantina de sua instituição.

  - Usuários do tipo **Vendedor** têm acesso a uma página onde podem editar as características dos produtos disponíveis para pedido, uma página de visualização geral dos produtos disponíveis em seu estabelecimento e uma página onde ele pode monitorar de forma rápida e prática os pedidos, com o objetivo de facilitar o monitoramento.

  - Cada produto deve ter um nome, uma descrição (onde pode conter os ingredientes ou outras informações), quantidade em estoque e valor do produto.
