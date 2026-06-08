package br.com.victorsfec.mom.model;

import java.io.Serializable;

public class Mensagem implements Serializable {

    // Necessário para que Publisher e Subscriber usem a mesma versão da classe na desserialização
    private static final long serialVersionUID = 1L;

    public String idSensor;
    public String topico;
    public double valor;

    public Mensagem(String idSensor, String topico, double valor) {
        this.idSensor = idSensor;
        this.topico   = topico;
        this.valor    = valor;
    }
}
