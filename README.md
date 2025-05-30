# CantinaIF
## Projeto Final de Web 2 - IF-CJO 2025

 <p>Este projeto visa desenvolver uma aplicação web para auxiliar o acesso a cantinas em instituições de ensino superior, onde muitas vezes as cantinas se encontram congestionadas em horário de intervalo ou troca de aulas. Esse projeto utiliza como principal ferramenta o acesso dos alunos a seus celulares ou computadores durante a aula, pois assim eles podem acessar um site onde o responsável pela cantina coloca os salgados, doces, bebidas e lanches especiáis disponíveis no dia. Com esse sistema, os alunos e colaboradores da instituição podem mandar um pedido informando ao vendedor que possuem interesse em comprar determinado item com antecedencia.</p>
  <p>Um sistema como esse oferece várias vantagens. Um exemplo é que o vendedor pode começar a preparar os alimentos enquanto os potenciais compradores ainda estão em suas aulas, e isso ajuda o vendedor a entender quantos e quais produtos deixar prontos, e também permite que os comprados deixem seu lanche reservado ao pedirem com antecedencia. A aplicação NÃO tem relação com entrega delivery de pedidos!</p>

#### Funcionamento da aplicação
  - Os usuários devem fornecer seu prontuário institucional, cpf e nome para que o cadastro seja realizado com sucesso; os documentos como prontuário e CPF serão verificados com as informações da instituição relacionada para confirmar que seja realmente um aluno ou colaborador da instituição, pois esse é um sistema com foco em escopo fechado, funcionando apenas dentro das faculdades e não tendo relação com clientes de fora, nem mesmo o aluno de uma faculdade que usa esse sistema porá usar seu login para comprar em uma faculdade diferente da sua, uma vez que cada cantina tera seu sistema fecho para sí e poderá ser acessada apenas pelos alunos e colaboradores de sua mesma instituição/unidade.<br><br>
    *De acordo com o prontuário usado para login, o sistema direciona o usuário para a cantina correspondente.*

  - Usuários do tipo **Comprador** têm acesso a uma página inicial que mostra os itens disponíveis na cantina de sua instituição.

  - Usuários do tipo **Vendedor** têm acesso a uma página onde podem editar as características dos produtos disponíveis para pedido, uma página de visualização geral dos produtos disponíveis em seu estabelecimento e uma página onde ele pode monitorar de forma rápida e prática os pedidos, com o objetivo de facilitar o monitoramento.

  - Cada produto deve ter um nome, uma descrição (onde pode conter os ingredientes ou outras informações), quantidade em estoque e valor do produto.
