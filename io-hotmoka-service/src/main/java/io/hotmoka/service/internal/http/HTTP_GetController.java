package io.hotmoka.service.internal.http;

import io.hotmoka.network.requests.TransactionRestRequestModel;
import io.hotmoka.network.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.responses.TransactionRestResponseModel;
import io.hotmoka.network.updates.ClassTagModel;
import io.hotmoka.network.updates.StateModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.TransactionReferenceModel;
import io.hotmoka.service.internal.services.GetService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("get")
public class HTTP_GetController {

    @Autowired
    private GetService nodeGetService;

    @GetMapping("/takamakaCode")
    public @ResponseBody TransactionReferenceModel getTakamakaCode() {
        return nodeGetService.getTakamakaCode();
    }

    @GetMapping("/manifest")
    public @ResponseBody StorageReferenceModel getManifest() {
        return nodeGetService.getManifest();
    }

    @PostMapping("/state")
    public @ResponseBody StateModel getState(@RequestBody StorageReferenceModel request) {
        return nodeGetService.getState(request);
    }

    @PostMapping("/classTag")
    public @ResponseBody ClassTagModel getClassTag(@RequestBody StorageReferenceModel request) {
        return nodeGetService.getClassTag(request);
    }

    @PostMapping("/request")
    public @ResponseBody
    TransactionRestRequestModel<?> getRequestAt(@RequestBody TransactionReferenceModel reference) {
        return nodeGetService.getRequest(reference);
    }

    @PostMapping("/response")
    public @ResponseBody TransactionRestResponseModel<?> getResponseAt(@RequestBody TransactionReferenceModel reference) {
        return nodeGetService.getResponse(reference);
    }

    @PostMapping("/polledResponse")
    public @ResponseBody TransactionRestResponseModel<?> getPolledResponseAt(@RequestBody TransactionReferenceModel reference) {
        return nodeGetService.getPolledResponse(reference);
    }

    @GetMapping("/signatureAlgorithmForRequests")
    public @ResponseBody
    SignatureAlgorithmResponseModel getSignatureAlgorithmForRequests() {
        return nodeGetService.getSignatureAlgorithmForRequests();
    }
}