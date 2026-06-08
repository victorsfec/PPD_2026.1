package br.com.victorsfec.mom.sensor;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;

import br.com.victorsfec.mom.model.Mensagem;

public class Sensor {

    public String id;
    public String parametro;
    public double limiteMinimo;
    public double limiteMaximo;
    public double valorAtual;

    private static final String URL_BROKER = "tcp://localhost:61616";

    public Sensor(String id, String parametro, double min, double max) {
        this.id           = id;
        this.parametro    = parametro;
        this.limiteMinimo = min;
        this.limiteMaximo = max;
    }

    // Atualiza a leitura e dispara alerta se o valor estiver fora dos limites
    public void setValor(double novoValor) {
        this.valorAtual = novoValor;
        if (this.valorAtual < limiteMinimo || this.valorAtual > limiteMaximo) {
            enviarAlertaParaActiveMQ();
        }
    }

    private void enviarAlertaParaActiveMQ() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(URL_BROKER);
            factory.setTrustAllPackages(true);

            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(this.parametro);

            MessageProducer producer = session.createProducer(topic);
            ObjectMessage objMessage = session.createObjectMessage(
                new Mensagem(this.id, this.parametro, this.valorAtual));
            producer.send(objMessage);

            session.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("Erro ao conectar o Sensor " + this.id + " ao ActiveMQ: " + e.getMessage());
        }
    }
}
