package io.hotmoka.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.CharValue;
import io.hotmoka.beans.values.DoubleValue;
import io.hotmoka.beans.values.FloatValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.ShortValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.marshalling.MarshallingContext;

public class Marshallable {


    @Test
    @DisplayName("writeShort(22)")
    public void testShort() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeShort(22);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcCABY=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeInt(32)")
    public void testInt() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeInt(32);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcEAAAAIA==", toBase64(bytes));
    }

    @Test
    @DisplayName("writeLong(92)")
    public void testLong() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeLong(92);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcIAAAAAAAAAFw=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeLong(1000129)")
    public void testLong2() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeLong(1000129);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcIAAAAAAAPQsE=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeLong(9007199254740991)")
    public void testLong3() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeLong(9007199254740991L);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcIAB////////8=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeBigInteger(9007199254740991L)")
    public void testBigInt() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeBigInteger(BigInteger.valueOf(9007199254740991L));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJAgAf////////", toBase64(bytes));
    }

    @Test
    @DisplayName("writeBigInteger(9)")
    public void testBigInt2() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeBigInteger(BigInteger.valueOf(9));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcBDQ==", toBase64(bytes));
    }

    @Test
    @DisplayName("writeBigInteger(7654319)")
    public void testBigInt3() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeBigInteger(BigInteger.valueOf(7654319));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcFAQB0y68=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeBigInteger(9007199254740991765896)")
    public void testBigInt4() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeBigInteger(new BigInteger("9007199254740991765896"));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcYAxY5MDA3MTk5MjU0NzQwOTkxNzY1ODk2", toBase64(bytes));
    }

    @Test
    @DisplayName("writeDouble(33.8)")
    public void testDouble() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeDouble(33.8);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcIQEDmZmZmZmY=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeFloat(33.8f)")
    public void testFloat() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeFloat(33.8f);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcEQgczMw==", toBase64(bytes));
    }

    @Test
    @DisplayName("writeBoolean(true)")
    public void testBoolean() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeBoolean(true);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcBAQ==", toBase64(bytes));
    }

    @Test
    @DisplayName("writeChar('d')")
    public void testChar() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeChar('d');
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcCAGQ=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeUTF(hello world)")
    public void testString() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeUTF("hello world");
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcNAAtoZWxsbyB3b3JsZA==", toBase64(bytes));
    }

    @Test
    @DisplayName("writeUTF(a very long string)")
    public void testVeryLongString() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeUTF("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");
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXoAAAQAFMxyTzBBQlhvQUFBUUFBd0FKWTJoaGFXNTBaWE4wLy8vUTVKWkdqQ1g4cFpGNWlGK254ZjlQUkE3NzBPREpiQ1FtdDVsek5tR1lnZ1FBRTRnQUQ2QUFCZjhBQUE5RFVFc0RCQlFBQUFnSUFDOTJlRk1LVzkzQVVBQUFBRkVBQUFBVUFBQUFUVVZVUVMxSlRrWXZUVUZPU1VaRlUxUXVUVWJ6VGN6TFRFc3RMdEVOU3kwcXpzelBzMUl3MURQZzVYSXVTazBzU1UzUmRhcTBVdkJOTEV2TlUvQktMRklJeUNsTno4eFRNTll6QXFseEtzM01TZEgxU3NuV0RTNUlUUVpxTk9UbDR1VUNBRkJMQXdRS0FBQUlBQUF2ZG5oVEFBQUFBQUFBQUFBQUFBQUFDUUFBQUUxRlZFRXRTVTVHTDFCTEF3UUtBQUFJQUFBdmRuaFRBQUFBQUFBQUFBQUFBQUFBQXdBQUFHbHZMMUJMQXdRS0FBQUlBQUF2ZG5oVEFBQUFBQUFBQUFBQUFBQUFDd0FBQUdsdkwyaHZkRzF2YTJFdlVFc0RCQW9BQUFnQUFDOTJlRk1BQUFBQUFBQUFBQUFBQUFBVUFBQUFhVzh2YUc5MGJXOXJZUzlsZUdGdGNHeGxjeTlRU3dNRUNnQUFDQUFBTG5aNFV3QUFBQUFBQUFBQUFBQUFBQndBQUFCcGJ5OW9iM1J0YjJ0aEwyVjRZVzF3YkdWekwyeGhiV0prWVhNdlVFc0RCQlFBQUFnSUFDNTJlRk5LNFRSS3dRc0FBS1FnQUFBcEFBQUFhVzh2YUc5MGJXOXJZUzlsZUdGdGNHeGxjeTlzWVcxaVpHRnpMMHhoYldKa1lYTXVZMnhoYzNPMVdRZDhHOVVaL3ovWjF0bnlPVkpFQm5HSU16Q0pMRHR4bkFsa0VNYzRpUlBIVG1NbndWQ2daL2xzWHl6ZEdla1VTR21oTGJTc2x1Nlc3bDA2NkFnbHl4UktkMHZwM292dXZRZmRwZnpmM2Ztc3lKSXNSdTJmVDkrOTk3N3YrMy9qdmU5NzhvT1BUdHdQWUtQWUdNSjYzRndGNTJkRkpJU0Z1S1VHWWR5cTRMWVFnbmloZkhtUmZOd3VIeThPOGZFU0JTK3R4Y3Z3OG1xOFFzRXJhOGp6S2puK2FqbDRSNVNDM2xpTE4rSE5VUWk4VmM2K0xZb0EzaEZGQmU2TW9sS09oZkZ1QmUrUnE5NGJ3bDE0WHhSVk9DWmY3NDVTNlFsSm5hekZLWnlXSWllaVVIQmZGTlY0SUlvYStRamh6bHA4SEorUVdqK3A0Rk5SMU9LQkVENkxCeVhuNXhROEZNSXFmRDRLRlY4SzhmRmx1ZnoAAAQAQXJVZFRobUtTK3J1QWJBdEc5WFQxZGV3L3N2YktyNTJCblgvL2V6cDUrZ1huZGg3VWpXbXRLczBkYnR4c2pYYWF0aitqcFRRSTFobmxFejloV09pTndicmRodGRyYW1KYmlYMnZDR3RKYnM3YVJiTzNqdERhaWR4c1pXM0wwR1NPbVptZlR1a0QzekJ5YnB5OUphdVpJNno3dHFEYVkxRHNzMDA1ckNYdlRWb3F1c3V4UlBTMXdudVFadGV5VVJSYjlHaTAxbnRRelpFc05EbW1aMW03M2srdURXc3JLbWpZeGpXY0hrMFppajM2VTlydW1Pa3I2N0xSaGpzaVZtdzNUc0xjS05NY0tlMkk2VTlOQmdjb080aFVJZHh1bTNwTk5EZXJwZmdsYUtyRVNXdktnbGpia3V6ZFlhWThhZE9PQy9jUmtwUFNEUnNiZ2VMdHBXclptRzViSnVjV2x2VUdralhvQUFBUUFSVmJzU0ZzcDMxbFVVc1FPaVRyb3hwU0U0ODgxZE93Ukxaa2x3aFZsQmtOZ3JrMEpycWNQR2Zhb2xiWDdIZU1xWWxKRGhXVlNXa1BCTlR0MXUwL2FtNkNYemx6Z1NxaVhnM3QxcmgvYXJ3L3JhZDFNNlAxV0p6VWZkY1IzQ1N3dnZxUjN1RmZhMUpIVU1wUlZQWm05SlhuNnRKUnZHa01nRi9JMVk2ZXpDYkpPQnhHYlljVSthM3kvbnNrbVplS042SGE3bDRSbng1cUs3VExGSGsxYjJaRlJnVlhGNGxhTU5UQm9NSkM2MU10QUx2SFluVjAybkRVVE1xOWFkM2lFRS8wS0Rnc3NLcmxPSGdobkpQRFI4Y2trTmtveGJpNXJZNWExU083M1pFbHJubEpsMGpQVkp0UFdEYkhyMExVOHdXSmxDV2dxODBRUzQ1T3kxd2xjV3RxNnd1ZFF1YjY3ZkFiZlBSbnAwbG5ocTBjTjJ6bkJPMGIxeEJnMzJ3V3huRU95ZC9Dd3pvT2luQkc1b3dOVzI1a25zemNuWjliSUIwTlJsN0RNaEdicnBuTllDbHdabTM0b0Y5Qlhqa203dTVvS1ZZVkFwazArSklETVd2bGcwTVJ1U2F3blFkZ0xYVUdOMDgreFJpNk54OG84VHFVL1Z4UVVsWDltTmhKR1E2bVZqUVM3clZnTkt4OU4yTlBobnArTmRFTnZlUnZoaWV0WUxXQ1hyK09wQkJMcXM3THBoTDdEa09lYjZnbFlKVjJvb2gzZlZQRXRmRnZGQnJhTldERkRQMk9PNlVOeVQwakdmU3JPeHdVcXRtS2ppb3V3VGNWMzhGMDJMMlVCVS9BOUZkL0h3d0xzSnM4dHcyQVZQNXoAAAQAQjYyckZkeFEveEk0SElkc3V5V2FLMGNiZnFaY0pCL0RpQ2gvbnhrd2l1VS9GVC9FekJ6MVg4QXIvazJLL2t3SzlWL0FZYitmWmIrZlk3RXIrWHhCL0NDdjZvWWpPMkNNd3BsRm9xL29RL0svaUxpci9pa1FnZUNWZmhieEZjb2VMditJZUtmK0pmRWZ5YlkvK0o0TDhxSHNYL1ZHNGZvWWlBS2lwd1FCV1Zvb3F6YlJIc3BqWVJERmNMSlNLcXlTMXF3b29JUlVTdElsUXA3QkZWMUlsWnRDMS9tNGVESWh3UkVVWE1Wa1ZVbktXS09XTHVHY3ZjUGEyS2VUakF0Zk9yeGRtcVdJQXM1VW1sWTlEQ1ZhSStBa1BGTTZBSnRCUU9VdWMxdHA0MnRXVHlhTy9WcGo3VW5raTRwWDF1Z2FOSVpwYmk5Rlc5dzdJZjIxMjBoQWRaYVpPeXIxMFdheXFTdGptZDE5SVpXMnJXZUcxb0tBL1Y1R0VyTzREK3poNXFaVzdvV29xSDJHUmY0c2h4UjZVUi9OZ1VFZ3ZGT2J3N2lFV3ltMDRrOUhGS041N0NiZHBVcUVySjNpcWJjbnd6dndneWdWbkRWcnBUUzR6MnBvZllmUTBWNjMxOFdVMEhwUjBOQWhlWFc5SkxJNnZvN2VtVUVoY0xyQy9kdEJVUklabVhzTitNemJ4c3FTeUJ3N0k3dkt4QVNJdEdUMkQyMUpTN21uVjZWbHEvS211azlSN0w3TWttazJ5TkMrWEo5Q0VKWkJudkN0cjRlUExvazNOalRxTlprZExZRTUwL1E5OWF3c1NDV2Q3Rkc3MDR0OFExaU1VNzVCalNudW1TTzdpeGNCVDZMYzdtb0swbVdtZHNxc3FXWWlpQW05TSs5UHJpczNRTXcwOXZEeHRwdWFQbnhBcEVSR0JIMlhvQUFBUUFMY2FNT2RZb3NLVm9IcGZYM1JaSlpkOGRVczE1anBySGtUblR6OHVGSlhUd3pFM3JDZDA0SXUvYnNTNjU2ZThTeTJmT2NiL25Xemd0SmgwV1QrZUUreFdJY3RneVRDNWtGMWJvMVBTWHlqdGR3bjBSV0IwcnRiSmdYSU5KM1J5eFIwTmloWWd4Q1lUemcva01zZ3dwdTJEWjhqbFhVTTIwTTlLdlRZcUlxNkpadEtxNEVjOVg4VHpjb09JNWVLNks2Mld4ZjdaOFhNNlNMRmF6NHJKTllOVzdERTlYUlp1c21tdFlETVZhRmFQZ1pYTFpGQjYyWnRhWTdrV0ViWVEyckVuTTNQMjFxYWszUmJBZERuWmIxbGlXVzFudE1rM3ZBcTdUWXcvbE90NlQ1ellrdXpSemlIRnZkQnoAAAQAa0xmTU5TakZIZVJwL1lwS3V5TkcvVDlOa08xdWMrM25ZMk9kOG8yQWZkTDB1cWJjc0Z5dE5qR29zNzQ4WnF4NlRUN3ZqLys4SmJYcnhNRkxSTEVSdlltSmFGalpsZmVoMldJb3oxQUtvd1d6Yk5wS0t5RVdZZmV5SHBBRGJ4ajYya003N1ZtMmQ3ek0renVZWU5MT2M3K0hZYkt2Z0xySSszbklTSXowSGdGQ3FQb3lwK0w0SURKNkVjUjNXOCtUaHE0aTNIRVlyZnc3Y1RxSDBBNm1uVUJYRE1VWFV4bncxUStGeEZPQ3NJTElaNnJNWml0S0VSYTdBT2E5SEoyU1d1SXV4d1lFbEtBaE1PSmFFRnNKTjBpSis3K05ubHlPWVc4b0RlUU82QS9GbzVIcDFWR21sMFZobFFOeUJJcUJGQ1hVaS9MYUhubHROekcrZzNDWFdlcThxSHV0R0RtZzlRWUk4UDhCNVU4aGU0NUY2RUIrNUdoQWhDSnpGNzc1bXZQZm5BNVB0cFJOa05yNXpBV2NCcHpNa2RuTUJjYjJ6S2ducGlCeEdGNmJZR09tNFYzZFRCaUI3aVU2S1B1emg4OUpkNDZNUEVMSE1pUUs0TDBVMnF3ckVvN0Zza3NEZUJIbjcwWWgrZjBxd0RuSlRPV0hBMzVuWDd1Sm9uTUg4YXJsbE9nSGRRKzA1bTJhNGNUeTd3c0ZUU21xYzVXQVQyK3lwa0xzbzBtT09MbnlCL3Z2U1FJMmtQZmRUdFNGWmRIayt5UUo4dnI4T1ROOWVYRjU4Z2hNSUNleWxpWDQ3QXViN0FmdHJ1Q2p6Q3ZKWnp1K0puQnZNNDZxdnV3OEtCQ2c3MURWVFNRMzJuY0E0bjZ3ZE9ZZEdoQ2ZxWlNoY0hhTThTU1MwTjhNSHU3cTQ4REgzY25QMDVHSGI1R0E3NlJxWG9QTG0yelRXS2JSY3pxTHVBK21aUGQyT1IrRnlDT2d4d2UxNmFFNTgyVDJFZHVRNDVDU0M0YnRKODB6Ti9XM25tbDIzMjVUVDdpaHl6dC9sbUQvaTZyNkxaY201akFVMFRPTTlYdEh3bWxmTWcveEUxeU1SS1lCbUcwQUtkMjMwd1IvMUdYLzJsOUxxN3FmZDU2aHNLcWw5UnhNVUd0UnpHSXQ1NHA2UTNlTkxsaGd0eXhEM2YyQjE0aHJZNlR1ZUJUdWZXZk5BWEdIUUd6UnhCVlQ1TWRob2V6QTE4a3g0Tk5PZHpwbk9pSFBCUGhBQ3U5TSt6U1RCQ1hzb0pYOG9iOERaOXVEbCtHckVBVXkxY2RRSk4wczZLbkFBZW9iMVg1eWdJK3dyQzlHeUNJb2Q4MkhKRTk4dlRjSnoAAAQAN3FFYW9PUEI3Vno2VHFhNStnYWlGYk1FL2RSWG9BQUFPQW5qcWxPVWd0OFNuM3VXcXVvN3V2ejFHaitHb1VSbmpMTkJlTytTNjh6Qk04dTVucE1tbUpVc0NTRzFpRmJzeFJNZHRYTVh1YUpYSWs2VnVTeWxOdXd2S1M2VWF2RnJVRTcwTjhvS0taU2R2U04xQzEwazNiWm0rdm5FTExhYXlVd0ZhZFFHditWcm1KU1h3emsvZ1cxc2hieVhOVFRtMXA4VEcyWUp4N1ZEaFVtbFRBb1RLa0tqaHFJK3NoTW9oVDF0MjY1cGFWOHlybkJpZDRBaUUvV1c5M1ZHeDF5bHVkcjZLT2ViYk5tYTlqdHJrcTZuQ05VNzRrZFpUSlVPbFExK0paQ0VZQ3NnLzMxRzd4b2xEVHpHM0ZQR283bHFleU44ZnpOYjdLR3ZjVWpJUmtYKytKV3VOVmxlbzRBMXBBMHA2Y1BWcnQ4SVA4dkJ4NC9CZDZVSUxOOFFMY08zTndCSjBhQ1llYXhNR2JoaWRucXljbkZIZk92eE5Za3k5cmU0NnNrTmNGU21wU0ZxOHVucXdlci91THhDZXJ4bG9wY1YyK3hQYWNCaTdpUzR3NFBZUndLQ2s3d1BrWE9KdzNpVldPUmlGYXhEcXhrc1gzTlh5L2xYdjJ0Vnp6T3J3ZWIvQ290L2pVMjMzcW5UNzFMbzk2UHo3QVpKSFVoOWhuSGZkbTc4V0h2ZG43OFJHUCtpZys1czNlNzg5K0dwL0JGNGp6aTk3N1YvRzF4d0JRU3dFQ0ZBTVVBQUFJQ0FBdmRuaFRDbHZkd0ZBQUFBQlJBQUFBRkFBQUFBQUFBQUFBQUFBQXBJRUFBQUFBVFVWVVFTMUpUa1l2VFVGT1NVWkZVMVF1VFVaUVN3RUNGQU1LQUFBSUFBQXZkbmhUQUFBQUFBQUFBQUFBQUFBQUNRQUFBQUFBQUFBQUFCQUE3VUdDQUFBQVRVVlVRUzFKVGtZdlVFc0JBaFFEQ2dBQUNBQUFMM1o0VXdBQUFBQUFBQUFBQUFBQUFBTUFBQUFBQUFBQUFBQVFBTzFCcVFBQUFHbHZMMUJMQVFJVUF3b0FBQWdBQUM5MmVGTUFBQUFBQUFBQUFBQUFBQUFMQUFBQUFBQUFBQUFBRUFEdFFjb0FBQUJwYnk5b2IzUnRiMnRoTDFCTEFRSVVBd29BQUFnQUFDOTJlRk1BQUFBQUFBQUFBQUFBQUFBVUFBQUFBQUFBQUFBQUVBRHRRZk1BQUFCcGJ5OW9iM1J0YjJ0aEwyVjRZVzF3YkdWekwxQkxBUUlVQXdvQUFBZ0FBQzUyZUZNQUFBQUFBQUFBQUFBQUFBQWNBQXfOQUFBQUFBQUFBQUVBRHRRU1VCQUFCcGJ5OW9iM1J0YjJ0aEwyVjRZVzF3YkdWekwyeGhiV0prWVhNdlVFc0JBaFFERkFBQUNBZ0FMblo0VTByaE5FckJDd0FBcENBQUFDa0FBQUFBQUFBQUFBQUFBS1NCWHdFQUFHbHZMMmh2ZEcxdmEyRXZaWGhoYlhCc1pYTXZiR0Z0WW1SaGN5OU1ZVzFpWkdGekxtTnNZWE56VUVzRkJnQUFBQUFIQUFjQXhnRUFBR2NOQUFBQUFBQT0=", toBase64(bytes));
    }

    @Test
    @DisplayName("write(hello world.getBytes())")
    public void testBytes() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.write("hello world".getBytes());
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcLaGVsbG8gd29ybGQ=", toBase64(bytes));
    }

    @Test
    @DisplayName("write(bytesOf(\"lambdas.jar\"))")
    public void testJarBytes() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.write(bytesOf("lambdas.jar"));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("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", toBase64(bytes));
    }

    @Test
    @DisplayName("writeCompactInt(30006)")
    public void testCompactInt() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeCompactInt(30006);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcF/wAAdTY=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeStringShared(Hotmoka)")
    public void testStringShared() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new MarshallingContext(baos)) {
            context.writeStringShared("Hotmoka");
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcK/wAHSG90bW9rYQ==", toBase64(bytes));
    }

    @Test
    @DisplayName("writeFieldSignature(classType fieldSignature)")
    public void testFieldSignature() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            FieldSignature fieldSignature = new FieldSignature(ClassType.CONTRACT, "balance", ClassType.BIG_INTEGER);

            context.writeObject(FieldSignature.class, fieldSignature);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcM/xQAB2JhbGFuY2Ua", toBase64(bytes));
    }

    @Test
    @DisplayName("writeStorageReference(storageReference)")
    public void testStorageReference() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageReference storageReference = new StorageReference(
            		new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            		new BigInteger("19992")
            );

            context.writeObject(StorageReference.class, storageReference);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcl///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggBOGA==", toBase64(bytes));
    }

    @Test
    @DisplayName("testWriteTransactionReference(transactionReference)")
    public void testTransactionReference() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            TransactionReference transactionReference = new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882");
            context.writeObject(TransactionReference.class, transactionReference);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXch/9DklkaMJfylkXmIX6fF/09EDvvQ4MlsJCa3mXM2YZiC", toBase64(bytes));
    }

    @Test
    @DisplayName("writeFieldSignature(basicType fieldSignature)")
    public void testFieldSignatureBasicType() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            FieldSignature fieldSignature = new FieldSignature(ClassType.STORAGE_TREE_INTMAP_NODE, "size", BasicTypes.INT);

            context.writeObject(FieldSignature.class, fieldSignature);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJ/yYABHNpemUE", toBase64(bytes));
    }

    @Test
    @DisplayName("new StringValue(\"hello\")")
    public void testStringValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageValue storageValue = new StringValue("hello");
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcICgAFaGVsbG8=", toBase64(bytes));
    }

    @Test
    @DisplayName("new StringValue(very long string)")
    public void testVeryLongStringValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageValue storageValue = new StringValue("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");
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXoAAAQAChTMck8wQUJYb0FBQVFBQXdBSlkyaGhhVzUwWlhOMC8vL1E1SlpHakNYOHBaRjVpRitueGY5UFJBNzcwT0RKYkNRbXQ1bHpObUdZZ2dRQUU0Z0FENkFBQmY4QUFBOURVRXNEQkJRQUFBZ0lBQzkyZUZNS1c5M0FVQUFBQUZFQUFBQVVBQUFBVFVWVVFTMUpUa1l2VFVGT1NVWkZVMVF1VFVielRjekxURXN0THRFTlN5MHF6c3pQczFJdzFEUGc1WEl1U2swc1NVM1JkYXEwVXZCTkxFdk5VL0JLTEZJSXlDbE56OHhUTU5ZekFxbHhLczNNU2RIMVNzbldEUzVJVFFacU5PVGw0dVVDQUZCTEF3UUtBQUFJQUFBdmRuaFRBQUFBQUFBQUFBQUFBQUFBQ1FBQUFFMUZWRUV0U1U1R0wxQkxBd1FLQUFBSUFBQXZkbmhUQUFBQUFBQUFBQUFBQUFBQUF3QUFBR2x2TDFCTEF3UUtBQUFJQUFBdmRuaFRBQUFBQUFBQUFBQUFBQUFBQ3dBQUFHbHZMMmh2ZEcxdmEyRXZVRXNEQkFvQUFBZ0FBQzkyZUZNQUFBQUFBQUFBQUFBQUFBQVVBQUFBYVc4dmFHOTBiVzlyWVM5bGVHRnRjR3hsY3k5UVN3TUVDZ0FBQ0FBQUxuWjRVd0FBQUFBQUFBQUFBQUFBQUJ3QUFBQnBieTlvYjNSdGIydGhMMlY0WVcxd2JHVnpMMnhoYldKa1lYTXZVRXNEQkJRQUFBZ0lBQzUyZUZOSzRUUkt3UXNBQUtRZ0FBQXBBQUFBYVc4dmFHOTBiVzlyWVM5bGVHRnRjR3hsY3k5c1lXMWlaR0Z6TDB4aGJXSmtZWE11WTJ4aGMzTzFXUWQ4RzlVWi96L1oxdG55T1ZKRUJuR0lNekNKTER0eG5BbGtFTWM0aVJQSFRtTW53VkNnWi9sc1h5emRHZWtVU0dtaExiU3NsdTZXN2wwNjZBZ2x5eFJLZDB2cDNvdnV2UWZkcGZ6ZjNmbXN5SklzUnUyZlQ5Kzk5Nzd2KzMvanZlOTc4b09QVHR3UFlLUFlHTUo2M0Z3RjUyZEZKSVNGdUtVR1lkeXE0TFlRZ25paGZIbVJmTnd1SHk4TzhmRVNCUyt0eGN2dzhtcThRc0VyYThqektqbithamw0UjVTQzNsaUxOK0hOVVFpOFZjNitMWW9BM2hGRkJlNk1vbEtPaGZGdUJlK1JxOTRid2wxNFh4UlZPQ1pmNzQ1UzZRbEpuYXpGS1p5V0lpZWlVSEJmRk5WNElJb2ErUWpoemxwOEhKK1FXaitwNEZOUjFPS0JFRDZMQnlYbjV4UThGTUlxZkQ0S0ZWOEs4ZkZsdXoAAAQAZkFyVWRUaG1LUytydUFiQXRHOVhUMWRldy9zdmJLcjUyQm5YLy9lenA1K2dYbmRoN1VqV210S3MwZGJ0eHNqWGFhdGoranBUUUkxaG5sRXo5aFdPaU53YnJkaHRkcmFtSmJpWDJ2Q0d0SmJzN2FSYk8zanREYWlkeHNaVzNMMEdTT21abWZUdWtEM3pCeWJweTlKYXVaSTZ6N3RxRGFZMURzczAwNXJDWHZUVm9xdXN1eFJQUzF3bnVRWnRleVVSUmI5R2kwMW50UXpaRXNORG1tWjFtNzNrK3VEV3NyS21qWXhqV2NIazBaaWozNlU5cnVtT2tyNjdMUmhqc2lWbXczVHNMY0tOTWNLZTJJNlU5TkJnY29PNGhVSWR4dW0zcE5ORGVycGZnbGFLckVTV3ZLZ2xqYmt1emRZYVk4YWRPT0MvY1JrcFBTRFJzYmdlTHRwV3JabUc1Ykp1Y1dsdlVHa2pYb0FBQVFBUlZic1NGc3AzMWxVVXNRT2lUcm94cFNFNDg4MWRPd1JMWmtsd2hWbEJrTmdyazBKcnFjUEdmYW9sYlg3SGVNcVlsSkRoV1ZTV2tQQk5UdDF1MC9hbTZDWHpsemdTcWlYZzN0MXJoL2Fydy9yYWQxTTZQMVdKelVmZGNSM0NTd3Z2cVIzdUZmYTFKSFVNcFJWUFptOUpYbjZ0SlJ2R2tNZ0YvSTFZNmV6Q2JKT0J4R2JZY1UrYTN5L25za21aZUtONkhhN2w0Um54NXFLN1RMRkhrMWIyWkZSZ1ZYRjRsYU1OVEJvTUpDNjFNdEFMdkhZblYwMm5EVVRNcTlhZDNpRUUvMEtEZ3NzS3JsT0hnaG5KUERSOGNra05rb3hiaTVyWTVhMVNPNzNaRWxybmxKbDBqUFZKdFBXRGJIcjBMVTh3V0psQ1dncTgwUVM0NU95MXdsY1d0cTZ3dWRRdWI2N2ZBYmZQUm5wMGxuaHEwY04yem5CTzBiMXhCZzMyd1d4bkVPeWQvQ3d6b09pbkJHNW93TlcyNWtuc3pjblo5YklCME5SbDdETWhHYnJwbk5ZQ2x3Wm0zNG9GOUJYamttN3U1b0tWWVZBcGswK0pJRE1XdmxnME1SdVNhd25RZGdMWFVHTjA4K3hSaTZOeDhvOFRxVS9WeFFVbFg5bU5oSkdRNm1WalFTN3JWZ05LeDlOMk5QaG5wK05kRU52ZVJ2aGlldFlMV0NYcitPcEJCTHFzN0xwaEw3RGtPZWI2Z2xZSlYyb29oM2ZWUEV0ZkZ2RkJyYU5XREZEUDJPTzZVTnlUMGpHZlNyT3h3VXF0bUtqaW91d1RjVjM4RjAyTDJVQlUvQTlGZC9Id3dMc0pzOHR3MkFWUHoAAAQANUI2MnJGZHhRL3hJNEhJZHN1eVdhSzBjYmZxWmNKQi9EaUNoL254a3dpdVUvRlQvRXpCejFYOEFyL2sySy9rd0s5Vi9BWWIrZlpiK2ZZN0VyK1h4Qi9DQ3Y2b1lqTzJDTXdwbEZvcS9vUS9LL2lMaXIvaWtRZ2VDVmZoYnhGY29lTHYrSWVLZitKZkVmeWJZLytKNEw4cUhzWC9WRzRmb1lpQUtpcHdRQldWb29xemJSSHNwallSREZjTEpTS3F5UzFxd29vSVJVU3RJbFFwN0JGVjFJbFp0QzEvbTRlRElod1JFVVhNVmtWVW5LV0tPV0x1R2N2Y1BhMktlVGpBdGZPcnhkbXFXSUFzNVVtbFk5RENWYUkrQWtQRk02QUp0QlFPVXVjMXRwNDJ0V1R5YU8vVnBqN1Vua2k0cFgxdWdhTklacGJpOUZXOXc3SWYyMTIwaEFkWmFaT3lyMTBXYXlxU3RqbWQxOUlaVzJyV2VHMW9LQS9WNUdFck80RCt6aDVxWlc3b1dvcUgyR1JmNHNoeFI2VVIvTmdVRWd2Rk9idzdpRVd5bTA0azlIRktONTdDYmRwVXFFckozaXFiY253enZ3Z3lnVm5EVnJwVFM0ejJwb2ZZZlEwVjYzMThXVTBIcFIwTkFoZVhXOUpMSTZ2bzdlbVVFaGNMckMvZHRCVVJJWm1Yc04rTXpieHNxU3lCdzdJN3ZLeEFTSXRHVDJEMjFKUzdtblY2VmxxL0ttdWs5UjdMN01rbWsyeU5DK1hKOUNFSlpCbnZDdHI0ZVBMb2szTmpUcU5aa2RMWUU1MC9ROTlhd3NTQ1dkN0ZHNzA0dDhRMWlNVTc1QmpTbnVtU083aXhjQlQ2TGM3bW9LMG1XbWRzcXNxV1lpaUFtOU0rOVByaXMzUU13MDl2RHh0cHVhUG54QXBFUkdCSDJYb0FBQVFBTGNhTU9kWW9zS1ZvSHBmWDNSWkpaZDhkVXMxNWpwckhrVG5Uejh1RkpYVHd6RTNyQ2QwNEl1L2JzUzY1NmU4U3kyZk9jYi9uV3pndEpoMFdUK2VFK3hXSWN0Z3lUQzVrRjFibzFQU1h5anRkd24wUldCMHJ0YkpnWElOSjNSeXhSME5paFlneENZVHpnL2tNc2d3cHUyRFo4amxYVU0yME05S3ZUWXFJcTZKWnRLcTRFYzlYOFR6Y29PSTVlSzZLNjJXeGY3WjhYTTZTTEZhejRySk5ZTlc3REU5WFJadXNtbXRZRE1WYUZhUGdaWExaRkI2Mlp0YVk3a1dFYllRMnJFbk0zUDIxcWFrM1JiQWREblpiMWxpV1cxbnRNazN2QXE3VFl3L2xPdDZUNXpZa3V6UnppSEZ2ZHoAAAQAQmtMZk1OU2pGSGVScC9ZcEt1eU5HL1Q5TmtPMXVjKzNuWTJPZDhvMkFmZEwwdXFiY3NGeXROakdvczc0OFpxeDZUVDd2ai8rOEpiWHJ4TUZMUkxFUnZZbUphRmpabGZlaDJXSW96MUFLb3dXemJOcEtLeUVXWWZleUhwQURieGo2MmtNNzdWbTJkN3pNK3p1WVlOTE9jNytIWWJLdmdMckkrM25JU0l6MEhnRkNxUG95cCtMNElESjZFY1IzVzgrVGhxNGkzSEVZcmZ3N2NUcUgwQTZtblVCWERNVVhVeG53MVErRnhGT0NzSUxJWjZyTVppdEtFUmE3QU9hOUhKMlNXdUl1eHdZRWxLQWhNT0phRUZzSk4waUorNytObmx5T1lXOG9EZVFPNkEvRm81SHAxVkdtbDBWaGxRTnlCSXFCRkNYVWkvTGFIbmx0TnpHK2czQ1hXZXE4cUh1dEdEbWc5UVlJOFA4QjVVOGhlNDVGNkVCKzVHaEFoQ0p6Rjc3NW12UGZuQTVQdHBSTmtOcjV6QVdjQnB6TWtkbk1CY2IyektnbnBpQnhHRjZiWUdPbTRWM2RUQmlCN2lVNktQdXpoODlKZDQ2TVBFTEhNaVFLNEwwVTJxd3JFbzdGc2tzRGVCSG43MFloK2YwcXdEbkpUT1dIQTM1blg3dUpvbk1IOGFybGxPZ0hkUSswNW0yYTRjVHk3d3NGVFNtcWM1V0FUMit5cGtMc28wbU9PTG55Qi92dlNRSTJrUGZkVHRTRlpkSGsreVFKOHZyOE9UTjllWEY1OGdoTUlDZXlsaVg0N0F1YjdBZnRydUNqekN2Slp6dStKbkJ2TTQ2cXZ1dzhLQkNnNzFEVlRTUTMybmNBNG42d2RPWWRHaENmcVpTaGNIYU04U1NTME44TUh1N3E0OERIM2NuUDA1R0hiNUdBNzZScVhvUExtMnpUV0tiUmN6cUx1QSttWlBkMk9SK0Z5Q09neHdlMTZhRTU4MlQyRWR1UTQ1Q1NDNGJ0Sjgwek4vVzNubWwyMzI1VFQ3aWh5enQvbG1EL2k2cjZMWmNtNWpBVTBUT005WHRId21sZk1nL3hFMXlNUktZQm1HMEFLZDIzMHdSLzFHWC8ybDlMcTdxZmQ1NmhzS3FsOVJ4TVVHdFJ6R0l0NTRwNlEzZU5MbGhndHl4RDNmMkIxNGhyWTZUdWVCVHVmV2ZOQVhHSFFHelJ4QlZUNU1kaG9lekExOGt4NE5OT2R6cG5PaUhQQlBoQUN1OU0relNUQkNYc29KWDhvYjhEWjl1RGwrR3JFQVV5MWNkUUpOMHM2S25BQWVvYjFYNXlnSSt3ckM5R3lDSW9kODJISkU5OHZUY3oAAAQASjdxRWFvT1BCN1Z6NlRxYTUrZ2FpRmJNRS9kUlhvQUFBT0FuanFsT1VndDhTbjN1V3F1bzd1dnoxR2orR29VUm5qTE5CZU8rUzY4ekJNOHU1bnBNbW1KVXNDU0cxaUZic3hSTWR0WE1YdWFKWElrNlZ1U3lsTnV3dktTNlVhdkZyVUU3ME44b0tLWlNkdlNOMUMxMGszYlptK3ZuRUxMYWF5VXdGYWRRR3YrVnJtSlNYd3prL2dXMXNoYnlYTlRUbTFwOFRHMllKeDdWRGhVbWxUQW9US2tLamhxSStzaE1vaFQxdDI2NXBhVjh5cm5CaWQ0QWlFL1dXOTNWR3gxeWx1ZHI2S09lYmJObWE5anRya3E2bkNOVTc0a2RaVEpVT2xRMStKWkNFWUNzZy8zMUc3eG9sRFR6RzNGUEdvN2xxZXlOOGZ6TmI3S0d2Y1VqSVJrWCsrSld1TlZsZW80QTFwQTBwNmNQVnJ0OElQOHZCeDQvQmQ2VUlMTjhRTGNPM053QkowYUNZZWF4TUdiaGlkbnF5Y25GSGZPdnhOWWt5OXJlNDZza05jRlNtcFNGcTh1bnF3ZXIvdUx4Q2VyeGxvcGNWMit4UGFjQmk3aVM0dzRQWVJ3S0NrN3dQa1hPSnczaVZXT1JpRmF4RHF4a3NYM05YeS9sWHYydFZ6ek9yd2ViL0NvdC9qVTIzM3FuVDcxTG85NlB6N0FaSkhVaDlobkhmZG03OFdIdmRuNzhSR1AraWcrNXMzZTc4OStHcC9CRjRqemk5NzdWL0cxeHdCUVN3RUNGQU1VQUFBSUNBQXZkbmhUQ2x2ZHdGQUFBQUJSQUFBQUZBQUFBQUFBQUFBQUFBQUFwSUVBQUFBQVRVVlVRUzFKVGtZdlRVRk9TVVpGVTFRdVRVWlFTd0VDRkFNS0FBQUlBQUF2ZG5oVEFBQUFBQUFBQUFBQUFBQUFDUUFBQUFBQUFBQUFBQkFBN1VHQ0FBQUFUVVZVUVMxSlRrWXZVRXNCQWhRRENnQUFDQUFBTDNaNFV3QUFBQUFBQUFBQUFBQUFBQU1BQUFBQUFBQUFBQUFRQU8xQnFRQUFBR2x2TDFCTEFRSVVBd29BQUFnQUFDOTJlRk1BQUFBQUFBQUFBQUFBQUFBTEFBQUFBQUFBQUFBQUVBRHRRY29BQUFCcGJ5OW9iM1J0YjJ0aEwxQkxBUUlVQXdvQUFBZ0FBQzkyZUZNQUFBQUFBQUFBQUFBQUFBQVVBQUFBQUFBQUFBQUFFQUR0UWZNQUFBQnBieTlvYjNSdGIydGhMMlY0WVcxd2JHVnpMMUJMQVFJVUF3b0FBQWdBQUM1MmVGTUFBQUFBQUFBQUFBQUFBQUFjQXfPQUFBQUFBQUFBQUFFQUR0UVNVQkFBQnBieTlvYjNSdGIydGhMMlY0WVcxd2JHVnpMMnhoYldKa1lYTXZVRXNCQWhRREZBQUFDQWdBTG5aNFUwcmhORXJCQ3dBQXBDQUFBQ2tBQUFBQUFBQUFBQUFBQUtTQlh3RUFBR2x2TDJodmRHMXZhMkV2WlhoaGJYQnNaWE12YkdGdFltUmhjeTlNWVcxaVpHRnpMbU5zWVhOelVFc0ZCZ0FBQUFBSEFBY0F4Z0VBQUdjTkFBQUFBQUE9", toBase64(bytes));
    }

    @Test
    @DisplayName("new IntValue(1993)")
    public void testIntValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageValue storageValue = new IntValue(1993);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcFDgAAB8k=", toBase64(bytes));
    }

    @Test
    @DisplayName("new BooleanValue(true)")
    public void testBooleanValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageValue storageValue = new BooleanValue(true);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcBAA==", toBase64(bytes));
    }

    @Test
    @DisplayName("new ByteValue(32)")
    public void testByteValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageValue storageValue = new ByteValue((byte) 32);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcCAiA=", toBase64(bytes));
    }

    @Test
    @DisplayName("new CharValue('D')")
    public void testCharValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageValue storageValue = new CharValue('D');
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcDAwBE", toBase64(bytes));
    }

    @Test
    @DisplayName("new ShortValue(44)")
    public void testShortValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageValue storageValue = new ShortValue((short) 44);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcDCQAs", toBase64(bytes));
    }

    @Test
    @DisplayName("new LongValue(1238769181L)")
    public void testLongValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageValue storageValue = new LongValue(1238769181L);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJBwAAAABJ1h4d", toBase64(bytes));
    }

    @Test
    @DisplayName("new DoubleValue(1238769181.9)")
    public void testDoubleValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageValue storageValue = new DoubleValue(1238769181.9);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJBEHSdYeHeZma", toBase64(bytes));
    }

    @Test
    @DisplayName("new FloatValue(23.7f)")
    public void testFloatValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StorageValue storageValue = new FloatValue(23.7f);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcFBUG9mZo=", toBase64(bytes));
    }

    @Test
    @DisplayName("new ConstructorCallTransactionRequest(..) manifest")
    public void testConstructorCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            ConstructorSignature constructorSignature = new ConstructorSignature(
                    ClassType.MANIFEST,
                    ClassType.BIG_INTEGER
            );

            ConstructorCallTransactionRequest constructorCall = new ConstructorCallTransactionRequest(
                   "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(11500),
                    BigInteger.valueOf(500),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    constructorSignature,
                    new BigIntegerValue(BigInteger.valueOf(999))
            );

            constructorCall.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdABAAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQALOwAAfQABQEGAAPnABMBGg==", toBase64(bytes));
    }

    @Test
    @DisplayName("new ConstructorSignature(..) manifest")
    public void testConstructorSignature() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            ConstructorSignature constructorSignature = new ConstructorSignature(
                    ClassType.MANIFEST,
                    ClassType.BIG_INTEGER
            );

            constructorSignature.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcEABMBGg==", toBase64(bytes));
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) balance of gasStation")
    public void testStaticMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            NonVoidMethodSignature nonVoidMethodSignature = new NonVoidMethodSignature(
                    ClassType.GAS_STATION,
                    "balance",
                    ClassType.BIG_INTEGER,
                    ClassType.STORAGE
            );

            StaticMethodCallTransactionRequest staticMethodCallTransactionRequest = new StaticMethodCallTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    nonVoidMethodSignature,
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
            );

            staticMethodCallTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdIBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQELAAEoARcAB2JhbGFuY2Ua", toBase64(bytes));
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) receive")
    public void testVoidStaticMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            MethodSignature receiveInt = new VoidMethodSignature(
                    ClassType.PAYABLE_CONTRACT,
                    "receive",
                    BasicTypes.INT
            );

            StaticMethodCallTransactionRequest staticMethodCallTransactionRequest = new StaticMethodCallTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    receiveInt,
                    new IntValue(300)
                );

            staticMethodCallTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdKBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQEOAAABLAIbAQQAB3JlY2VpdmU=", toBase64(bytes));
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) nonce")
    public void testNonVoidStaticMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            StaticMethodCallTransactionRequest staticMethodCallTransactionRequest = new StaticMethodCallTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    CodeSignature.NONCE
            );

            staticMethodCallTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdDBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEgAABW5vbmNlGg==", toBase64(bytes));
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) receive")
    public void testVoidInstanceMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            MethodSignature receiveInt = new VoidMethodSignature(
                    ClassType.PAYABLE_CONTRACT,
                    "receive",
                    BasicTypes.INT
            );

            InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    receiveInt,
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    new IntValue(300)
            );

            request.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXc8BwAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQAAAAEs", toBase64(bytes));
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) getGamete")
    public void testNonVoidInstanceMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    CodeSignature.GET_GAMETE,
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
            );

            request.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdRBQAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEwAACWdldEdhbWV0ZQr/AAZHYW1ldGUA", toBase64(bytes));
    }

    @Test
    @DisplayName("new JarStoreTransactionRequest(..)")
    public void testJarStoreTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
            JarStoreTransactionRequest jarStoreTransactionRequest = new JarStoreTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    bytesOf("lambdas.jar")
            );

            jarStoreTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("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"
                , toBase64(bytes));
    }

    protected static String toBase64(byte[] bytes) {
       return new String(Base64.getEncoder().encode(bytes));
    }

    public static byte[] bytesOf(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get("./jars/" + fileName));
    }
}