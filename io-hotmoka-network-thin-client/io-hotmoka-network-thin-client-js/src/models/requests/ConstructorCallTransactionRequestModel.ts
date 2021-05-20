import {NonInitialTransactionRequestModel} from "./NonInitialTransactionRequestModel";
import {ConstructorSignatureModel} from "../signatures/ConstructorSignatureModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

export class ConstructorCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    constructorSignature: ConstructorSignatureModel
    actuals: Array<StorageValueModel>
    chainId: string
    signature: string


    constructor(caller: StorageReferenceModel,
                nonce: string,
                classpath: TransactionReferenceModel,
                gasLimit: string,
                gasPrice: string,
                constructorSignature: ConstructorSignatureModel,
                actuals: Array<StorageValueModel>,
                chainId: string,
                signature: string) {
        super(caller, nonce, classpath, gasLimit, gasPrice)
        this.constructorSignature = constructorSignature
        this.actuals = actuals
        this.chainId = chainId
        this.signature = signature
    }

    protected into(context: MarshallingContext): void {
        //TODO
    }

    protected intoWithoutSelector(context: MarshallingContext): void {
        //TODO
    }

}