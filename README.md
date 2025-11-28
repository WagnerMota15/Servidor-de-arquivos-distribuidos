# Servidor-de-arquivos-distribu-dos
Atividade da disciplina de Sistemas Distribuídos, problema resolvido:

Implementar um servidor de arquivos. Funcionalidades que devem ser implementadas: 
a) O cliente irá abrir uma conexão TCP/IP via unicast com o servidor principal solicitando ao mesmo os nomes e endereços(IP) dos servidores de arquivos que contem um arquivo em especifico. 

b) Assim que o servidor principal recebe a solicitação do(s) cliente(s) o mesmo envia uma mensagem multicast ou broadcast na rede direcionada aos servidores de arquivos perguntando quais desses possuem o arquivo solicitado pelo cliente. Nesse momento o servidor principal determina um timeout de 10 segundos para receber a resposta dos servidores de arquivos que possuem aquele determinado arquivo. Obs: O Servidor principal pode receber várias solicitações ao mesmo tempo de vários clientes  

c) Assim que o servidor de arquivo recebe uma solicitação o mesmo verifica em seu diretório padrão se o arquivo solicitado existe em sua base de arquivos. Caso exista, envia uma mensagem para o servidor principal informando que possui aquele determinado arquivo caso não possua não irá fazer nada.  Obs: O servidor de arquivos pode receber vários pedidos de pesquisas do servidor principal ao mesmo tempo.  

d) O servidor principal guarda em uma coleção todos os endereços de todos os servidores de arquivos que informaram possuir aquele arquivo e ao termino do timeout responde ao cliente informando todos os servidores de arquivos que possuem o arquivo solicitado. 

e) Assim que o cliente receber a lista dos servidores de arquivos que possuem o determinado arquivo, o mesmo irá escolher um dos servidores de arquivo que desejar baixar através de uma conexão TCP/IP via unicast com o servidor que possua o arquivo. Obs: O servidor de arquivo pode enviar vários arquivos ao mesmo tempo para vários clientes. 
