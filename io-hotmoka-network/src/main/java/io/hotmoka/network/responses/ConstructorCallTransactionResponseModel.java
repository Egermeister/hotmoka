package io.hotmoka.network.responses;

import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;

public abstract class ConstructorCallTransactionResponseModel extends CodeExecutionTransactionResponseModel {

	protected ConstructorCallTransactionResponseModel(ConstructorCallTransactionResponse response) {
        super(response);
    }

    protected ConstructorCallTransactionResponseModel() {}
}