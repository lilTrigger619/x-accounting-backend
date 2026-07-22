package com.unionsg.xaccounting.communication.email;

import com.unionsg.xaccounting.communication.dto.EmailSendResult;

public interface MailService {

    EmailSendResult send(EmailMessage message);
}
