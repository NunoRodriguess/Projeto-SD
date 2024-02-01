# Makefile para compilar e executar o programa Java

# Caminho para as dependências JAR
DEPENDENCIES = -cp .:comon/sd23.jar

# Comando para compilar
COMPILE = javac $(DEPENDENCIES)

# Comando para executar o programa Worker
RUN_WORKER = java $(DEPENDENCIES) comon.worker.Worker

# Comando para executar o programa Cliente
RUN_CLIENTE = java $(DEPENDENCIES) comon.cliente.Cliente

# Comando para executar o programa Servidor com um número de threads
RUN_SERVIDOR = java $(DEPENDENCIES) comon.servidor.Servidor

# Alvo para compilar
compile:
	$(COMPILE) comon/servidor/Servidor.java comon/servidor/PedidodoBuffer.java comon/servidor/Manager.java comon/servidor/ClienteHandler.java comon/servidor/BoundedQueue.java comon/worker/Worker.java comon/Mensagem.java comon/cliente/Cliente.java comon/cliente/ClienteView.java comon/cliente/ClienteController.java

# Alvo para executar o programa Worker
worker:
	$(RUN_WORKER)

# Alvo para executar o programa Cliente
cliente:
	$(RUN_CLIENTE)

# Alvo para executar o programa Servidor
servidor:
	$(RUN_SERVIDOR)

# Alvo para limpar arquivos compilados
clean:
	rm -rf comon/servidor/*.class comon/worker/*.class comon/cliente/*.class comon/*.class

