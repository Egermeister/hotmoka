import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {MethodCallTransactionRequestModel} from "./MethodCallTransactionRequestModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {MethodSignatureModel} from "../signatures/MethodSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

export class InstanceSystemMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
    receiver: StorageReferenceModel

    constructor(
        caller: StorageReferenceModel,
        nonce: string,
        classpath: TransactionReferenceModel,
        gasLimit: string,
        gasPrice: string,
        method: MethodSignatureModel,
        actuals: Array<StorageValueModel>,
        receiver: StorageReferenceModel
    ) {
        super(caller, nonce, classpath, gasLimit, gasPrice, method, actuals)
        this.receiver = receiver
    }

    protected into(context: MarshallingContext): void {
        //TODO
    }

    protected intoWithoutSelector(context: MarshallingContext): void {
        //TODO
    }
}