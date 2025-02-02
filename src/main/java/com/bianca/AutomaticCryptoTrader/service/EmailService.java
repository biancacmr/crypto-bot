package com.bianca.AutomaticCryptoTrader.service;

import com.bianca.AutomaticCryptoTrader.config.BinanceConfig;
import com.bianca.AutomaticCryptoTrader.indicators.Indicators;
import com.bianca.AutomaticCryptoTrader.model.Order;
import com.bianca.AutomaticCryptoTrader.model.OrderResponse;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Service
public class EmailService {
    private BinanceService binanceService;
    private BinanceConfig config;
    private Indicators indicators;

    public EmailService(BinanceConfig config, BinanceService binanceService, Indicators indicators) {
        this.config = config;
        this.binanceService = binanceService;
        this.indicators = indicators;
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

    public void sendEmailOrder(List<String> emailReceiverList, OrderResponse order) throws MessagingException {
        String emailSubject = getOrderEmailSubject(order);

        String orderBlock = montaHtmlDadosOrdem(order);
        String indicatorsBlock = montaHtmlDadosIndicadores();
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
                .append("<br>")
                .append(indicatorsBlock)
                .append("<br>")
                .append(lastOrdersBlock)
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
            String datetimeTransact = formatDate(lastBuyOrder.getTime());
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
                    .append("<tr><th>Valor na Compra</th><td>")
                    .append(binanceService.adjustToTickStr(valueInUSDT))
                    .append("</td></tr>")
                    .append("<tr><th>Valor em USDT</th><td>")
                    .append(binanceService.adjustToTickStr(totalValue))
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
            String datetimeTransact = formatDate(lastSellOrder.getTime());
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
                    .append("<tr><th>Valor na Compra</th><td>")
                    .append(binanceService.adjustToTickStr(valueInUSDT))
                    .append("</td></tr>")
                    .append("<tr><th>Valor em USDT</th><td>")
                    .append(binanceService.adjustToTickStr(totalValue))
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
        String html = "<h2>📊 Indicadores no momento da operação </h2>" +
                "<table>" +
                "<tr><th>MA (" + config.getMaFastWindow() + ")</th><td>" +
                indicators.getMaFast().getLast() +
                "</td><td>" +
                "GRADIENTE: " + getGradienteMa("FAST") +
                "</td></tr>" +
                "<tr><th>MA (" + config.getMaSlowWindow() + ")</th><td>" +
                indicators.getMaSlow().getLast() +
                "</td><td>" +
                "GRADIENTE: " + getGradienteMa("SLOW") +
                "</td></tr>" +
                "<tr><th>RSI (" + config.getRsiWindow() + ")</th><td>" +
                indicators.getRsi().getLast() +
                "</td><td>" +
                "CONDIÇÃO: " + getCondicaoRsi() +
                "</td></tr>" +
                "</table>";
        return html;
    }

    private String getGradienteMa(String type) {
        if (type.equals("FAST")) {
            if (indicators.getMaFastGradient() > 0) return "SUBINDO 📈";
            return "DESCENDO 📉";
        } else if (type.equals("SLOW")) {
            if (indicators.getMaSlowGradient() > 0) return "SUBINDO 📈";
            return "DESCENDO 📉";
        }

        throw new RuntimeException("Value invalid for MA type");
    }

    private String getCondicaoRsi() {
        double rsi = indicators.getRsi().getLast();

        if (rsi > 70) return "SOBRECOMPRADO 🟢";
        if (rsi < 30) return "SOBREVENDIDO 🔴";
        return "NORMAL 🟠";
    }

    private String getOrderEmailSubject(OrderResponse order) {
        String tipoOrdem = order.getSide().trim().equalsIgnoreCase("BUY") ? "Compra" : "Venda";
        OrderResponse.Fill filledTransaction = order.getFills().getFirst();

        return "Ordem enviada de " + tipoOrdem + " - " + order.getSymbol()
                + " " + order.getExecutedQty() + "/" + filledTransaction.getPrice();
    }

    public String montaHtmlDadosOrdem(OrderResponse order) {
        if (order.getSchemaType().equals(OrderResponse.OrderResponseType.FULL)) {
            OrderResponse.Fill filledTransaction = order.getFills().getFirst();
        }

        String formattedDate = formatDate(order.getTransactTime());

        double quantity = Double.parseDouble(order.getExecutedQty());
        String tipo = order.getSide().equals("BUY") ? "Compra" : "Venda";
        double price = Double.parseDouble(filledTransaction.getPrice());
        double assetPrice = Double.parseDouble(order.getCumulativeQuoteQty());

        String html = "<h2>💲 " + getOrderEmailSubject(order) + "</h2>" +
                "<table>" +
                "<tr><th>Side</th><td>" + order.getSide() + "</td></tr>" +
                "<tr><th>Ativo</th><td>" + order.getSymbol() + "</td></tr>" +
                "<tr><th>Quantidade</th><td>" + binanceService.adjustToStepStr(quantity) + "</td></tr>" +
                "<tr><th>Moeda</th><td>" + filledTransaction.getCommissionAsset() + "</td></tr>" +
                "<tr><th>Valor em " + filledTransaction.getCommissionAsset() + "</th><td>" + binanceService.adjustToTickStr(assetPrice) + "</td></tr>" +
                "<tr><th>Valor na " + tipo + "</th><td>" + binanceService.adjustToTickStr(price) + "</td></tr>" +
                "<tr><th>Tipo</th><td>" + order.getType() + "</td></tr>" +
                "<tr><th>Status</th><td>" + order.getStatus() + "</td></tr>" +
                "<tr><th>Data/Hora</th><td>" + formattedDate + "</td></tr>" +
                "</table>";

        return html;
    }

    public String formatDate(long timestamp) {
        // Converter timestamp para uma data/hora legível no fuso horário UTC-3
        LocalDateTime dateTimeTransact = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("UTC-3"))  // Aqui, estamos ajustando para o fuso horário correto
                .toLocalDateTime();

        // Formatar no padrão dd/MM/yyyy - HH:mm
        return dateTimeTransact.format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"));
    }

    private Timestamp convertToUtcMinus3(Timestamp timestamp) {
        Instant instant = timestamp.toInstant();
        ZonedDateTime utcMinus3 = instant.atZone(ZoneId.of("UTC-3"));
        LocalDateTime localDateTime = utcMinus3.toLocalDateTime();
        return Timestamp.valueOf(localDateTime);
    }


}
