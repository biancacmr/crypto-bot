package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.model.Order;
import com.bianca.AutomaticCryptoTrader.model.OrderResponseFull;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Service
public class EmailService {

    private final BinanceConfig config;

    @Autowired
    private final BinanceService binanceService;

    public EmailService(BinanceConfig config, BinanceService binanceService) {
        this.config = config;
        this.binanceService = binanceService;
    }

    public void sendEmail(List<String> receivers, String subject, String body) throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", config.getEmailHost());
        prop.put("mail.smtp.port", config.getEmailPort());
        prop.put("mail.smtp.ssl.trust", config.getEmailHost());

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getEmailUsername(), config.getEmailPassword());
            }
        });

        for (String receiver : receivers) {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getEmailUsername()));
            message.setRecipients(
                    Message.RecipientType.TO, InternetAddress.parse(receiver));
            message.setSubject(subject);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(body, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart);

            Transport.send(message);
        }
    }

    public void sendEmailOrder(List<String> emailReceiverList, OrderResponseFull order) throws MessagingException {
        String emailSubject = getOrderEmailSubject(order);

        // Criando bloco do e-mail com dados da ordem
        String orderBlock = montaHtmlDadosOrdem(order);

        // Criando bloco do e-mail com dados dos indicadores
        String indicatorsBlock = montaHtmlDadosIndicadores();

        // Criando bloco do e-mail com as últimas ordens realizadas
        String lastOrdersBlock = montaHtmlHistoricoOrdens();

        // Criando e-mail completo
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html>")
                .append("<html lang=\"en\">")
                .append("<head>")
                .append("<meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>Ordem Enviada</title>")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }")
                .append(".email-container { background-color: #ffffff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); }")
                .append("h2 { color: #333; }")
                .append("table { width: 100%; margin-top: 20px; border-collapse: collapse; }")
                .append("table, th, td { border: 1px solid #ddd; }")
                .append("th, td { padding: 12px; text-align: left; }")
                .append("th { background-color: #f4f4f4; color: #333; }")
                .append("td { background-color: #fafafa; }")
                .append(".divider { margin-top: 20px; border-top: 2px solid #ddd; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class=\"email-container\">")
                .append(orderBlock)
                .append("</div>")
                .append("</body>")
                .append("</html>");

        sendEmail(emailReceiverList, emailSubject, htmlContent.toString());
    }

    private String montaHtmlHistoricoOrdens() {
        StringBuilder htmlBuilder = new StringBuilder();

        // Gerar tabela para a última ordem de compra
        Optional<Order> lastBuyOrderOpt = binanceService.getLastExecutedOrder("BUY");
        if (lastBuyOrderOpt.isPresent()) {
            Order lastBuyOrder = lastBuyOrderOpt.get();
            String datetimeTransact = binanceService.formatTimestamp(lastBuyOrder.getTime());
            double quantity = Double.parseDouble(lastBuyOrder.getOrigQty());
            double valueInUSDT = Double.parseDouble(lastBuyOrder.getCumulativeQuoteQty()) /
                    Double.parseDouble(lastBuyOrder.getExecutedQty());
            double totalValue = Double.parseDouble(lastBuyOrder.getCumulativeQuoteQty());

            htmlBuilder.append("<h2>✅ Última ordem de COMPRA executada para ")
                    .append(config.getOperationCode())
                    .append("</h2>")
                    .append("<table>")
                    .append("<tr><th>Data/Hora</th><td>")
                    .append(datetimeTransact)
                    .append("</td></tr>")
                    .append("<tr><th>Quantidade</th><td>")
                    .append(binanceService.adjustToStepStr(quantity))
                    .append("</td></tr>")
                    .append("<tr><th>Valor em USDT</th><td>")
                    .append(binanceService.adjustToStepStr(valueInUSDT))
                    .append("</td></tr>")
                    .append("<tr><th>Valor na Compra</th><td>")
                    .append(binanceService.adjustToStepStr(totalValue))
                    .append("</td></tr>")
                    .append("</table><br>");
        } else {
            htmlBuilder.append("<h2>✅ Última ordem de COMPRA executada para ")
                    .append(config.getOperationCode())
                    .append("</h2>")
                    .append("<p>Sem ordens de compra executadas.</p><br>");
        }

        // Gerar tabela para a última ordem de venda
        Optional<Order> lastSellOrderOpt = binanceService.getLastExecutedOrder("SELL");
        if (lastSellOrderOpt.isPresent()) {
            Order lastSellOrder = lastSellOrderOpt.get();
            String datetimeTransact = binanceService.formatTimestamp(lastSellOrder.getTime());
            double quantity = Double.parseDouble(lastSellOrder.getOrigQty());
            double valueInUSDT = Double.parseDouble(lastSellOrder.getCumulativeQuoteQty()) /
                    Double.parseDouble(lastSellOrder.getExecutedQty());
            double totalValue = Double.parseDouble(lastSellOrder.getCumulativeQuoteQty());

            htmlBuilder.append("<h2>❎ Última ordem de VENDA executada para ")
                    .append(config.getOperationCode())
                    .append("</h2>")
                    .append("<table>")
                    .append("<tr><th>Data/Hora</th><td>")
                    .append(datetimeTransact)
                    .append("</td></tr>")
                    .append("<tr><th>Quantidade</th><td>")
                    .append(binanceService.adjustToStepStr(quantity))
                    .append("</td></tr>")
                    .append("<tr><th>Valor em USDT</th><td>")
                    .append(binanceService.adjustToStepStr(valueInUSDT))
                    .append("</td></tr>")
                    .append("<tr><th>Valor na Compra</th><td>")
                    .append(binanceService.adjustToStepStr(totalValue))
                    .append("</td></tr>")
                    .append("</table>");
        } else {
            htmlBuilder.append("<h2>❎ Última ordem de VENDA executada para ")
                    .append(config.getOperationCode())
                    .append("</h2>")
                    .append("<p>Sem ordens de venda executadas.</p>");
        }

        return htmlBuilder.toString();
    }

    private String montaHtmlDadosIndicadores() {
        // TODO:: implementar
        return null;
    }

    private String getOrderEmailSubject(OrderResponseFull order) {
        String tipoOrdem = order.getSide().trim().equalsIgnoreCase("BUY") ? "Compra" : "Venda";
        OrderResponseFull.Fill filledTransaction = order.getFills().getFirst();

        return  "Ordem enviada de " + tipoOrdem + " - " + order.getSymbol()
                + " " + order.getExecutedQty() + "/" + filledTransaction.getPrice();
    }

    public String montaHtmlDadosOrdem(OrderResponseFull order) {
        OrderResponseFull.Fill filledTransaction = order.getFills().getFirst();
        String formattedDate = formatDate(order.getTransactTime());

        String html = "<h2>💲 " + getOrderEmailSubject(order) +  "</h2>" +
                "<table>" +
                "<tr><th>Side</th><td>" + order.getSide() + "</td></tr>" +
                "<tr><th>Ativo</th><td>" + order.getSymbol() + "</td></tr>" +
                "<tr><th>Quantidade</th><td>" + order.getExecutedQty() + "</td></tr>" +
                "<tr><th>Moeda</th><td>" + filledTransaction.getCommissionAsset() + "</td></tr>" +
                "<tr><th>Valor em " + filledTransaction.getCommissionAsset() + "</th><td>" + order.getCumulativeQuoteQty() + "</td></tr>" +
                "<tr><th>Valor na Venda</th><td>" + filledTransaction.getPrice() + "</td></tr>" +
                "<tr><th>Tipo</th><td>" + order.getType() + "</td></tr>" +
                "<tr><th>Status</th><td>" + order.getStatus() + "</td></tr>" +
                "<tr><th>Data/Hora</th><td>" + formattedDate + "</td></tr>" +
                "</table>";

        return html;
    }

    public String formatDate(long timestamp) {
        // Converter timestamp para uma data/hora legível
        LocalDateTime dateTimeTransact = Instant.ofEpochMilli(timestamp)
                .atOffset(ZoneOffset.UTC)
                .toLocalDateTime();

        // Formatar no padrão dd/MM/yyyy - HH:mm
        return dateTimeTransact.format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"));
    }

}
