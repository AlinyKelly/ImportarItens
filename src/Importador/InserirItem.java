package Importador;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.modelcore.comercial.CentralFinanceiro;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;

import java.math.BigDecimal;
import java.util.Date;

public class InserirItem implements AcaoRotinaJava {
    @Override
    public void doAction(ContextoAcao contextoAcao) throws Exception {

        Registro[] linhasSelecionadas = contextoAcao.getLinhas();

        try {

            for (Registro linha : linhasSelecionadas) {
                BigDecimal nunota = (BigDecimal) linha.getCampo("NUNOTA");
                String projeto = (String) linha.getCampo("PROJETO");
                Date dtprev = (Date) linha.getCampo("DTPREV");
                BigDecimal quantidade = (BigDecimal) linha.getCampo("QUANTIDADE");
                BigDecimal vlrunit = (BigDecimal) linha.getCampo("VLRUNITARIO");
                BigDecimal localorig = (BigDecimal) linha.getCampo("LOCALORIG");
                BigDecimal codprod = (BigDecimal) linha.getCampo("CODPROD");

                Registro newTgfIte = contextoAcao.novaLinha("ItemNota");
                newTgfIte.setCampo("NUNOTA", nunota);
                newTgfIte.setCampo("AD_PROJPROD", projeto);
                newTgfIte.setCampo("DTINICIO", dtprev);
                newTgfIte.setCampo("QTDNEG", quantidade);
                newTgfIte.setCampo("CODPROD", codprod);
                newTgfIte.setCampo("VLRUNIT", vlrunit);
                newTgfIte.setCampo("CODLOCALORIG", localorig);
                newTgfIte.setCampo("CODVOL", "UN");
                newTgfIte.setCampo("VLRTOT", quantidade.multiply(vlrunit));
                newTgfIte.setCampo("VLRDESC", 0);
                newTgfIte.setCampo("PERCDESC", 0);
                newTgfIte.setCampo("ATUALESTOQUE", 1);

                newTgfIte.save();

                recalcularImpostos(nunota);

            }
            //Finalmente configuramos uma mensagem para ser exibida após a execução da ação.
            //Finalmente configuramos uma mensagem para ser exibida após a execução da ação.
            StringBuffer mensagem = new StringBuffer();
            mensagem.append("Itens inseridos com sucesso.");
            contextoAcao.setMensagemRetorno(mensagem.toString());


        } catch (Exception e) {
            contextoAcao.setMensagemRetorno(String.valueOf(e));
        }

    }

    /**
     * Recalcula os impostos dos itens e da nota
     * @author Aliny Sousa
     * @param nunota  Numero da nota para recalculo
     */
    public static void recalcularImpostos(BigDecimal nunota) throws Exception {
        final ImpostosHelpper impostosHelper = new ImpostosHelpper();
        impostosHelper.calcularImpostos(nunota);
        impostosHelper.totalizarNota(nunota);

//        final CentralFinanceiro centralFinanceiro = new CentralFinanceiro();
//        centralFinanceiro.inicializaNota(nunota);
//        centralFinanceiro.refazerFinanceiro();
    }
}
