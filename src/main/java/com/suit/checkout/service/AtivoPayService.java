package com.suit.checkout.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import com.suit.checkout.models.Pagamentos;
import com.suit.checkout.models.dtos.*;
import com.suit.checkout.models.dtos.wl.DataCallbackDTO;
import com.suit.checkout.models.dtos.wl.ResponseData;
import com.suit.checkout.models.dtos.wl.WhiteLabelCallBackDTO;
import com.suit.checkout.models.enums.StatusPagamento;
import com.suit.checkout.repositories.PagamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class AtivoPayService {

    @Autowired
    private PaymentService paymentService;

    private static final String SecretKey = "sk_live_53Svv9U3khDlVD60N65FiXkhfi533bmD8MtKU3h4ib";
    private static final String callbackUrl = "https://lojashvnbr.com/api/payment/callback";
    private static final String postUrlHorizon = "https://api.conta.ativopay.com/v1/transactions";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GenerateQRCode generateQRCode;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    public Object createPaymentHorizon(RequestApiPaymentDTO data) throws IOException, WriterException {
        Pagamentos pagamentoModel = paymentService.createPayment(data);

        Integer valorApagar = (int) (data.valorAPagar() * 100);
        ItemsHorizonDTO itemsHorizonDTO = new ItemsHorizonDTO("TENIS AZUL", valorApagar, 1, false);
        HorizonDocumentRequestDTO horizonDocumentRequestDTO = new HorizonDocumentRequestDTO(data.cpf(), "cpf");
        ClientRequestHorizon clientRequestHorizon = new ClientRequestHorizon(data.nomePagador(), data.email(), horizonDocumentRequestDTO);
        HorizonRequestPaymentDTO horizonRequestPaymentDTO = new HorizonRequestPaymentDTO(valorApagar, "pix", clientRequestHorizon, List.of(itemsHorizonDTO), callbackUrl);

        String auth = SecretKey + ":x";


        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());


        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("Content-Type", "application/json");



        String json;
        try {
            json = new ObjectMapper().writeValueAsString(horizonRequestPaymentDTO);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter para JSON.");
        }

        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        ResponseEntity<ResponseData> responseEntity = restTemplate.exchange(postUrlHorizon, HttpMethod.POST, entity, ResponseData.class);

        ResponseData responseData = responseEntity.getBody();
        pagamentoModel.setIdTransactionAtivoPay(responseData.id());
        pagamentoRepository.save(pagamentoModel);
        String codeBase64 = generateQRCode.generateQRCodeBase64(responseData.pix().qrcode());
        ResponsePIX responsePIX = new ResponsePIX(responseData.id(), codeBase64, responseData.pix().qrcode(), "Pagamento com Horizon");


        return responsePIX;
    }

    public void callback(WhiteLabelCallBackDTO data) {
        Pagamentos pagamento = pagamentoRepository.findPagamentosByIdTransactionAtivoPay(data.data().id());
        if (pagamento == null) {
            throw new RuntimeException("Pagamento não encontrado");
        }
        System.out.println(data.data().status());
        if(data.data().status().equals("paid")) {
            pagamento.setStatusPagamento(StatusPagamento.PAGO);
            pagamento.setDataPagamento(LocalDateTime.now());
            pagamentoRepository.save(pagamento);
        }
        if (data.data().status().equals("expired")) {
            pagamento.setStatusPagamento(StatusPagamento.CANCELADO);
            pagamentoRepository.save(pagamento);
        }
    }
}
