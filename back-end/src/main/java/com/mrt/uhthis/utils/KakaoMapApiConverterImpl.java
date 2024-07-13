package com.mrt.uhthis.utils;

import com.mrt.uhthis.dto.KakaoMapResponse;
import com.mrt.uhthis.dto.OpenApiResponse;
import com.mrt.uhthis.dto.TrashBinResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KakaoMapApiConverterImpl implements KakaoMapApiConverter {

    private final WebClient webClient;
    private final OpenApiConverter openApiConverter;

    @Value("${kakao.api-key}")
    private String apiKey;

    public KakaoMapApiConverterImpl(ClientHttpConnector clientHttpConnector, OpenApiConverter openApiConverter) {
        this.webClient = WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .clientConnector(clientHttpConnector)
                .build();
        this.openApiConverter = openApiConverter;
    }

    @Override
    public List<TrashBinResponseDTO> convertAddrToPoint() {
        List<OpenApiResponse.TrashBinData> trashBinsInfo = openApiConverter.getTrashBinsInfo();

        if (trashBinsInfo == null || trashBinsInfo.isEmpty())
            throw new IllegalStateException("공공데이터가 존재하지 않습니다.");

        List<TrashBinResponseDTO> trashBinResponseDTOList = new ArrayList<>();
        for (OpenApiResponse.TrashBinData trashBin : trashBinsInfo) {
            KakaoMapResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/keyword")
                            .queryParam("query", trashBin.getAddress())
                            .queryParam("size", 1)
                            .build())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .bodyToMono(KakaoMapResponse.class)
                    .block();

            if (response == null || response.getDocuments() == null)
                throw new IllegalStateException("카카오맵 데이터가 존재하지 않습니다.");

            if (!response.getDocuments().isEmpty()) {
                KakaoMapResponse.Document document = response.getDocuments().get(0);

                TrashBinResponseDTO responseDTO = TrashBinResponseDTO.builder()
                        .address(trashBin.getAddress())
                        .description(trashBin.getDescription())
                        .longitude(Double.parseDouble(document.getX()))
                        .latitude(Double.parseDouble(document.getY()))
                        .binType(trashBin.getBinType().substring(2))
                        .build();

                trashBinResponseDTOList.add(responseDTO);
            }
        }

        return trashBinResponseDTOList;
    }
}