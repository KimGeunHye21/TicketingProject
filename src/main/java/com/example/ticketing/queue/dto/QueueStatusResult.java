package com.example.ticketing.queue.dto;

import com.example.ticketing.dto.queue.QueueStatusResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString(exclude = "admissionToken")
public class QueueStatusResult {

    private final QueueStatusResponse response;

    // SELECTING일 때만 존재
    private final AdmissionToken admissionToken;

    public static QueueStatusResult withoutToken(
            QueueStatusResponse response
    ) {
        return new QueueStatusResult(response, null);
    }

    public static QueueStatusResult withToken(
            QueueStatusResponse response,
            AdmissionToken admissionToken
    ) {
        return new QueueStatusResult(response, admissionToken);
    }

    public boolean hasAdmissionToken() {
        return admissionToken != null;
    }
}