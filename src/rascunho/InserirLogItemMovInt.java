package rascunho;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

import java.math.BigDecimal;

public class InserirLogItemMovInt implements AcaoRotinaJava {
    @Override
    public void doAction(ContextoAcao contextoAcao) throws Exception {

        Registro[] linhasSelecionadas = contextoAcao.getLinhas();

        BigDecimal nunota = null;
        BigDecimal codprod = null;
        Double quantidadeItem = 0.0;
        String codvol = "";
        String projeto = "";

        try {

            for (Registro linha : linhasSelecionadas) {
                nunota = (BigDecimal) linha.getCampo("NUNOTA");
                codprod = (BigDecimal) linha.getCampo("CODPROD");
                quantidadeItem = (Double) linha.getCampo("QUANTIDADE");
                codvol = (String) linha.getCampo("CODVOL");
                projeto = (String) linha.getCampo("PROJETO");

            }
            //Finalmente configuramos uma mensagem para ser exibida após a execução da ação.
            StringBuffer mensagem = new StringBuffer();
            mensagem.append("Itens inseridos com sucesso.");
            contextoAcao.setMensagemRetorno(mensagem.toString());

        } catch (Exception e) {
            contextoAcao.setMensagemRetorno(String.valueOf(e));
        }

    }

}
