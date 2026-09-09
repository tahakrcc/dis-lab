package tr.kopru.domain.caze;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tr.kopru.common.ApiException;
import tr.kopru.domain.catalog.*;
import tr.kopru.domain.caze.dto.CaseItemInputDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PriceListEntryRepository priceListEntryRepository;
    private final ServiceItemRepository serviceItemRepository;

    public List<CaseItem> buildCaseItems(
            DentalCase dentalCase,
            List<CaseItemInputDto> inputItems,
            LocalDate targetDate,
            boolean allowCustomPrice) {

        List<CaseItem> items = new ArrayList<>();

        for (CaseItemInputDto input : inputItems) {
            ServiceItem serviceItem = serviceItemRepository.findById(input.getServiceItemId())
                    .orElseThrow(() -> ApiException.notFound("Katalog kalemi bulunamadi: " + input.getServiceItemId()));

            validateItemUnits(serviceItem, input);

            BigDecimal birimFiyat;
            if (allowCustomPrice && input.getCustomBirimFiyat() != null) {
                birimFiyat = input.getCustomBirimFiyat();
            } else {
                PriceListEntry priceEntry = priceListEntryRepository
                        .findValidPrice(dentalCase.getPartnership().getId(), serviceItem.getId(), targetDate)
                        .orElseThrow(() -> ApiException.priceNotFound(
                                "Fiyat listesinde bu kalem icin gecerli bir fiyat bulunamadi: " + serviceItem.getAd(),
                                Map.of(
                                        "serviceItemId", serviceItem.getId(),
                                        "serviceItemAd", serviceItem.getAd(),
                                        "partnershipId", dentalCase.getPartnership().getId()
                                )
                        ));
                birimFiyat = priceEntry.getFiyat();
            }

            int adet = input.getAdet();
            if (serviceItem.getBirim() == BirimTipi.DIS) {
                adet = input.getDisNumaralari().length;
            }

            BigDecimal araToplam = birimFiyat.multiply(BigDecimal.valueOf(adet));

            CaseItem caseItem = new CaseItem();
            caseItem.setDentalCase(dentalCase);
            caseItem.setServiceItem(serviceItem);
            caseItem.setDisNumaralari(input.getDisNumaralari() != null ? input.getDisNumaralari() : new int[0]);
            caseItem.setMateryal(input.getMateryal());
            caseItem.setRenk(input.getRenk());
            caseItem.setAdet(adet);
            caseItem.setBirimFiyat(birimFiyat);
            caseItem.setAraToplam(araToplam);
            if (input.getSpecs() != null) {
                caseItem.setSpecs(input.getSpecs());
            }

            items.add(caseItem);
        }

        return items;
    }

    public BigDecimal calculateTotal(List<CaseItem> items) {
        return items.stream()
                .map(CaseItem::getAraToplam)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateItemUnits(ServiceItem serviceItem, CaseItemInputDto input) {
        if (serviceItem.getBirim() == BirimTipi.DIS) {
            if (input.getDisNumaralari() == null || input.getDisNumaralari().length == 0) {
                throw ApiException.validationError("Birim tipi DIS olan kalemler icin dis numaralari bos olamaz: " + serviceItem.getAd(), null);
            }
            if (input.getAdet() != input.getDisNumaralari().length) {
                // Auto-sync adet with tooth count or enforce equality
                input.setAdet(input.getDisNumaralari().length);
            }
        } else if (serviceItem.getBirim() == BirimTipi.ADET) {
            if (input.getDisNumaralari() != null && input.getDisNumaralari().length > 0) {
                throw ApiException.validationError("Birim tipi ADET olan kalemler icin dis numaralari girilemez: " + serviceItem.getAd(), null);
            }
        }
    }
}
