package io.hotmoka.network.internal.services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.hotmoka.network.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.models.requests.GameteCreationTransactionRequestModel;
import io.hotmoka.network.models.requests.InitializationTransactionRequestModel;
import io.hotmoka.network.models.requests.InstanceMethodCallTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreTransactionRequestModel;
import io.hotmoka.network.models.requests.RedGreenGameteCreationTransactionRequestModel;
import io.hotmoka.network.models.requests.StaticMethodCallTransactionRequestModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

@Service
public class AddServiceImpl extends AbstractService implements AddService {

	@Override
	public TransactionReferenceModel addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {
		return wrapExceptions(() -> new TransactionReferenceModel(getNode().addJarStoreInitialTransaction(request.toBean())));
	}

    @Override
    public StorageReferenceModel addGameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addGameteCreationTransaction(request.toBean())));
    }

    @Override
    public StorageReferenceModel addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addRedGreenGameteCreationTransaction(request.toBean())));
    }

    @Override
    public ResponseEntity<Void> addInitializationTransaction(InitializationTransactionRequestModel request) {
        return wrapExceptions(() -> {
            getNode().addInitializationTransaction(request.toBean());
            return ResponseEntity.noContent().build();
        });
    }

    @Override
    public TransactionReferenceModel addJarStoreTransaction(JarStoreTransactionRequestModel request) {
        return wrapExceptions(() -> new TransactionReferenceModel(getNode().addJarStoreTransaction(request.toBean())));
    }

    @Override
    public StorageReferenceModel addConstructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addConstructorCallTransaction(request.toBean())));
    }

    @Override
    public StorageValueModel addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageValueModel(getNode().addInstanceMethodCallTransaction(request.toBean())));
    }

    @Override
    public StorageValueModel addStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageValueModel(getNode().addStaticMethodCallTransaction(request.toBean())));
    }
}