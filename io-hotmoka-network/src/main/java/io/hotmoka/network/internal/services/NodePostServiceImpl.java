package io.hotmoka.network.internal.services;

import org.springframework.stereotype.Service;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.function.StorageValueMapper;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.JarStoreTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.MethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.TransactionReferenceModel;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.network.json.JSONTransactionReference;

@Service
public class NodePostServiceImpl extends AbstractNetworkService implements NodePostService {

    @Override
    public TransactionReferenceModel postJarStoreTransaction(JarStoreTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = decodeBase64(request.getSignature());
            byte[] jar = decodeBase64(request.getJar());
            StorageReference caller = request.getCaller().toBean();
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

            return new TransactionReferenceModel(getNode().postJarStoreTransaction(new JarStoreTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            jar,
                            dependencies)).get());
        });
    }

    @Override
    public StorageReferenceModel postConstructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = decodeBase64(request.getSignature());
            StorageReference caller = request.getCaller().toBean();
            ConstructorSignature constructor = new ConstructorSignature(request.getConstructorType(), StorageResolver.resolveStorageTypes(request.getValues()));
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

            return new StorageReferenceModel(getNode().postConstructorCallTransaction(new ConstructorCallTransactionRequest(
            	signature,
                caller,
                request.getNonce(),
                request.getChainId(),
                request.getGasLimit(),
                request.getGasPrice(),
                classpath,
                constructor,
                actuals)).get());
        });
    }

    @Override
    public StorageValueModel postInstanceMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = request.getCaller().toBean();
            StorageReference receiver = request.getReceiver().toBean();
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

            return responseOf(
                    getNode().postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            methodSignature,
                            receiver,
                            actuals)).get(),
                    new StorageValueMapper()
            );
        });
    }

    @Override
    public StorageValueModel postStaticMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = request.getCaller().toBean();
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

            return responseOf(
                    getNode().postStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            methodSignature,
                            actuals)).get(),
                    new StorageValueMapper()
            );
        });
    }
}
