package tr.kopru.domain.caze.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateCaseItemsRequest {
    @NotEmpty(message = "En az bir vaka kalemi olmalidir")
    @Valid
    private List<CaseItemInputDto> items;
}
