package br.com.victorsfec.mom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.advisory.DestinationSource;
import org.apache.activemq.command.ActiveMQTopic;

import br.com.victorsfec.mom.model.Mensagem;

public class Cliente {

    public String id;

    private static final String URL_BROKER = "tcp://localhost:61616";

    // Callback injetado pela GUI para exibir mensagens no terminal de logs
    private Consumer<String> atualizadorDeTela;

    public Cliente(String id, Consumer<String> atualizadorDeTela) {
        this.id                = id;
        this.atualizadorDeTela = atualizadorDeTela;
    }

    // Consulta o Broker e retorna os tópicos ativos, excluindo os internos do ActiveMQ
    public List<String> listarTopicosDisponiveis() {
        List<String> topicos = new ArrayList<>();
        Connection conn = null;
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(URL_BROKER);
            factory.setTrustAllPackages(true);

            conn = factory.createConnection();
            conn.start();

            DestinationSource ds = ((ActiveMQConnection) conn).getDestinationSource();

            // Aguarda o Advisory Topics popular a lista de destinos de forma assíncrona
            Thread.sleep(300);

            Set<ActiveMQTopic> brokerTopics = ds.getTopics();
            for (ActiveMQTopic topic : brokerTopics) {
                String nome = topic.getTopicName();
                if (!nome.startsWith("ActiveMQ.Advisory")) {
                    topicos.add(nome);
                }
            }

        } catch (Exception e) {
            atualizadorDeTela.accept(" Erro ao consultar tópicos no Broker: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
        return topicos;
    }

    // Registra um MessageListener assíncrono no Broker para receber alertas do tópico
    public void assinar(String topicoNome) {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(URL_BROKER);
            factory.setTrustAllPackages(true);

            // Conexão mantida aberta indefinidamente para escuta contínua
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(topicoNome);
            MessageConsumer consumer = session.createConsumer(topic);

            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof ObjectMessage) {
                            Mensagem msgMOM = (Mensagem) ((ObjectMessage) message).getObject();
                            atualizadorDeTela.accept(String.format(
                                " [ALERTA ActiveMQ] Cliente: %s | Origem: Sensor %s | Tópico: %s | Valor Atual: %.2f",
                                id, msgMOM.idSensor, msgMOM.topico, msgMOM.valor));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            atualizadorDeTela.accept(" Cliente " + this.id + " assinou o tópico '" + topicoNome + "' com sucesso!");

        } catch (Exception e) {
            atualizadorDeTela.accept(" Erro ao conectar o Cliente " + this.id + ": " + e.getMessage());
        }
    }
}
