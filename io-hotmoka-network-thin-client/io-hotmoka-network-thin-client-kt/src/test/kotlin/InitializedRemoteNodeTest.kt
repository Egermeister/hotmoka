
import io.hotmoka.network.thin.client.RemoteNode
import io.hotmoka.network.thin.client.RemoteNodeClient
import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.exceptions.TransactionRejectedException
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.JarStoreInitialTransactionResponseModel
import io.hotmoka.network.thin.client.models.responses.JarStoreTransactionSuccessfulResponseModel
import io.hotmoka.network.thin.client.models.responses.TransactionRestResponseModel
import io.hotmoka.network.thin.client.models.signatures.ConstructorSignatureModel
import io.hotmoka.network.thin.client.models.signatures.MethodSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import io.hotmoka.network.thin.client.suppliers.JarSupplier
import io.hotmoka.network.thin.client.webSockets.StompClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InitializedRemoteNodeTest {
    private val url = "localhost:8080"
    private val nonExistingTransactionReference = TransactionReferenceModel(
        "local",
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    )
    private val nonExistingStorageReference = StorageReferenceModel(nonExistingTransactionReference, "2")
    private val eventModel = EventRequestModel(
        StorageReferenceModel(
            TransactionReferenceModel("local", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
            "0"
        ),
        StorageReferenceModel(
            TransactionReferenceModel("local", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
            "0"
        )
    )

    private val takamakaJarVersion = "1.0.0"
    private val chainId = "io.hotmoka.runs.StartNetworkServiceWithInitializedMemoryNodeAndEmptySignature"
    private var nonce = 1

    /**
     * The gamete storage reference to set
     */
    private val gamete = StorageReferenceModel(
        TransactionReferenceModel("local", ""),
        "0"
    )




    @Test fun getTakamakaCode() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCode = service.getTakamakaCode()

            assertNotNull(takamakaCode, "expected takamakaCode to be not null")
            assertNotNull(takamakaCode.hash, "expected takamakaCode hash to be not null")
            assertEquals("local", takamakaCode.type)
        }
    }

    @Test fun getSignatureAlgorithmForRequests() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val algorithm = service.getSignatureAlgorithmForRequests()

            assertNotNull(algorithm)
            assertEquals("empty", algorithm.algorithm)
        }
    }

    @Test fun getManifest() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val manifest = service.getManifest()

            assertNotNull(manifest, "expected result to be not null")
            assertNotNull(manifest.transaction, "expected transaction to be not null")
            assertEquals("local", manifest.transaction.type)
        }
    }

    @Test fun getState() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val manifestReference = service.getManifest()
            val state = service.getState(manifestReference)

            assertNotNull(state, "expected state to be not null")
            assertEquals(2, state.updates.size)
            assertNotNull(state.updates[0].updatedObject, "expected updateObject to not null")
            assertEquals(manifestReference.transaction.hash, state.updates[0].updatedObject.transaction.hash)
        }
    }

    @Test fun getStateNonExisting() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.getState(nonExistingStorageReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun getClassTag() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val manifestReference = service.getManifest()
            val classTag = service.getClassTag(manifestReference)

            assertNotNull(classTag, "expected classTag to be not null")
            assertEquals("io.takamaka.code.system.Manifest", classTag.className)
            assertNotNull(classTag.jar.hash, "expected classTag jar to be not null")
        }
    }

    @Test fun getClassTagNonExisting() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.getClassTag(nonExistingStorageReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun getRequest() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val transactionRequest = service.getRequest(service.getTakamakaCode())

            assertNotNull(transactionRequest, "expected transactionRequest to be not null")
            assertTrue(
                transactionRequest.transactionResponseModel is JarStoreInitialTransactionRequestModel,
                "expected transaction request model to be of type JarStoreInitialTransactionResponseModel"
            )
        }
    }


    @Test fun getRequestNonExisting() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.getRequest(nonExistingTransactionReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }


    @Test fun getResponse() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val transactionResponse = service.getResponse(service.getTakamakaCode())

            assertNotNull(transactionResponse, "expected transactionResponse to be not null")
            assertTrue(
                transactionResponse.transactionResponseModel is JarStoreInitialTransactionResponseModel,
                "expected transaction response model to be of type JarStoreInitialTransactionResponseModel"
            )
        }
    }

    @Test fun getResponseNonExisting() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.getRequest(nonExistingTransactionReference)
            } catch (e: Exception) {
                assertTrue(e is NoSuchElementException, "expected exception to of type NoSuchElementException")
                assertTrue(e.message!!.equals("unknown transaction reference 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun getResponseFailed() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {

                // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
                // this means that the request fails and the future refers to a failed request; since this is a post,
                // the execution does not stop, nor throws anything
                val jarSupplier: JarSupplier
                try {
                    jarSupplier = service.postJarStoreTransaction(
                        JarStoreTransactionRequestModel(
                            "",
                            this.gamete,
                            getIncrementedGameteNonce(),
                            service.getTakamakaCode(),
                            this.chainId,
                            "20000",
                            "1",
                            getJarExampleOf("lambdas"),
                            listOf()
                        )
                    )

                } catch (e: Exception) {
                    fail("unexpected exception")
                }

                // we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
                // since the request failed and its transaction was rejected
                try {
                    jarSupplier.get()
                } catch (e: Exception) {

                }

                // if we ask for the outcome of the request, we will get the TransactionRejectedException as answer
                service.getResponse(jarSupplier.getReferenceOfRequest())

            } catch (e: TransactionRejectedException) {
                assertTrue(e.message!!.contains("io.takamaka.code.verification.IncompleteClasspathError"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun getPolledResponse() {
        val transactionResponse: TransactionRestResponseModel<*>

        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCodeRef = service.getTakamakaCode()

            val jarSupplier = service.postJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    nonce++.toString(),
                    takamakaCodeRef,
                    this.chainId,
                    "20000",
                    "1",
                    getJarExampleOf("lambdas"),
                    listOf(takamakaCodeRef)
                )
            )

            transactionResponse = service.getPolledResponse(jarSupplier.getReferenceOfRequest())
        }

        assertNotNull(transactionResponse)
        assertNotNull(transactionResponse.transactionResponseModel)
        assertTrue(transactionResponse.transactionResponseModel is JarStoreTransactionSuccessfulResponseModel)
    }

    @Test fun getPolledResponseNonExisting() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.getPolledResponse(nonExistingTransactionReference)
            } catch (e: Exception) {
                assertTrue(e is TimeoutException, "expected exception to of type TimeoutException")
                return
            }

            fail("expected exception")
        }
    }

    @Test fun getPolledResponseFailed() {
        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {

                // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
                // this means that the request fails and the future refers to a failed request; since this is a post,
                // the execution does not stop, nor throws anything
                val jarSupplier: JarSupplier
                try {
                    jarSupplier = service.postJarStoreTransaction(
                        JarStoreTransactionRequestModel(
                            "",
                            this.gamete,
                            getIncrementedGameteNonce(),
                            service.getTakamakaCode(),
                            this.chainId,
                            "20000",
                            "1",
                            getJarExampleOf("lambdas"),
                            listOf()
                        )
                    )

                } catch (e: Exception) {
                    fail("unexpected exception")
                }

                // we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
                // since the request failed and its transaction was rejected
                try {
                    jarSupplier.get()
                } catch (e: Exception) {

                }

                // if we ask for the outcome of the request, we will get the TransactionRejectedException as answer
                service.getPolledResponse(jarSupplier.getReferenceOfRequest())

            } catch (e: TransactionRejectedException) {
                assertTrue(e.message!!.contains("io.takamaka.code.verification.IncompleteClasspathError"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun addJarStoreInitialTransaction() {

        val nodeService = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.addJarStoreInitialTransaction(
                    JarStoreInitialTransactionRequestModel(
                        getJarTestOf("c13"),
                        listOf(service.getTakamakaCode())
                    )
                )

            } catch (e: Exception) {
                assertTrue(
                    e is TransactionRejectedException,
                    "expected exception to of type TransactionRejectedException"
                )
                assertTrue(e.message!!.equals("cannot run a JarStoreInitialTransactionRequest in an already initialized node"))
                return
            }

            fail("expected exception")
        }
    }


    @Test fun addJarStoreTransaction() {

        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCode = service.getTakamakaCode()
            val transaction = service.addJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    nonce++.toString(),
                    takamakaCode,
                    this.chainId,
                    "20000",
                    "1",
                    getJarTestOf("c13"),
                    listOf(takamakaCode)
                )
            )

            assertNotNull(transaction)
        }
    }


    @Test fun addJarStoreTransactionRejected() {

        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            val incorrectClasspath = TransactionReferenceModel("local", "")

            try {
                service.addJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        getIncrementedGameteNonce(),
                        incorrectClasspath,
                        this.chainId,
                        "20000",
                        "1",
                        getJarTestOf("c13"),
                        listOf()
                    )
                )
            } catch (e: Exception) {
                assertTrue(
                    e is TransactionRejectedException,
                    "expected exception to of type TransactionRejectedException"
                )
                assertTrue(e.message!!.equals("io.takamaka.code.verification.IncompleteClasspathError: java.lang.ClassNotFoundException: io.takamaka.code.lang.Contract"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun addJarStoreTransactionFailed() {

        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCodeRef = service.getTakamakaCode()
            try {
                service.addJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        nonce++.toString(),
                        takamakaCodeRef,
                        this.chainId,
                        "20000",
                        "1",
                        getJarExampleOf("callernotonthis"),
                        listOf(takamakaCodeRef)
                    )
                )
            } catch (e: Exception) {
                assertTrue(e is TransactionException, "expected exception to of type TransactionRejectedException")
                assertTrue(e.message!!.contains("io.takamaka.code.verification.VerificationException"))
                assertTrue(e.message!!.contains("caller() can only be called on \"this\""))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun postJarStoreTransaction() {
        val jarTransaction: TransactionReferenceModel

        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCodeRef = service.getTakamakaCode()
            val jarSupplier = service.postJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    nonce++.toString(),
                    takamakaCodeRef,
                    this.chainId,
                    "20000",
                    "1",
                    getJarExampleOf("lambdas"),
                    listOf(takamakaCodeRef)
                )
            )

            jarTransaction = jarSupplier.get()
        }

        assertNotNull(jarTransaction)
    }

    @Test fun postJarStoreTransactionRejected() {

        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
                // this means that the request fails and the future refers to a failed request; since this is a post,
                // the execution does not stop, nor throws anything
                val jarSupplier = service.postJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        getIncrementedGameteNonce(),
                        service.getTakamakaCode(),
                        this.chainId,
                        "20000",
                        "1",
                        getJarExampleOf("lambdas"),
                        listOf()
                    )
                )

                // we wait until the request has been processed; this will throw a TransactionRejectedException at the end,
                // since the request failed and its transaction was rejected
                jarSupplier.get()

            } catch (e: Exception) {
                assertTrue(
                    e is TransactionRejectedException,
                    "expected exception to of type TransactionRejectedException"
                )
                assertTrue(e.message!!.equals("io.takamaka.code.verification.IncompleteClasspathError: java.lang.ClassNotFoundException: io.takamaka.code.lang.Contract"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun postJarStoreTransactionFailed() {
        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCodeRef = service.getTakamakaCode()
            try {

                // we try to install a jar, but we forget to add its dependency (lambdas.jar needs takamakaCode() as dependency);
                // this means that the request fails and the future refers to a failed request; since this is a post,
                // the execution does not stop, nor throws anything
                val jarSupplier = service.postJarStoreTransaction(
                    JarStoreTransactionRequestModel(
                        "",
                        this.gamete,
                        nonce++.toString(),
                        takamakaCodeRef,
                        this.chainId,
                        "20000",
                        "1",
                        getJarExampleOf("callernotonthis"),
                        listOf(takamakaCodeRef)
                    )
                )

                jarSupplier.get()

            } catch (e: TransactionException) {
                assertTrue(e.message!!.contains("io.takamaka.code.verification.VerificationException"))
                assertTrue(e.message!!.contains("caller() can only be called on \"this\""))
                return
            }

            fail("expected exception")
        }
    }



    @Test fun runStaticMethodCallTransaction() {
        val toString: StorageValueModel?

        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            val takamakaCodeRef = service.getTakamakaCode()
            val jar = service.addJarStoreTransaction(
                JarStoreTransactionRequestModel(
                    "",
                    this.gamete,
                    nonce++.toString(),
                    takamakaCodeRef,
                    this.chainId,
                    "20000",
                    "1",
                    getJarExampleOf("javacollections"),
                    listOf(takamakaCodeRef)
                )
            )

            val nonVoidMethodSignature = MethodSignatureModel(
                "testToString1",
                "java.lang.String",
                listOf(),
                "io.takamaka.tests.javacollections.HashMapTests"
            )

            toString = service.runStaticMethodCallTransaction(
                StaticMethodCallTransactionRequestModel(
                    "",
                    this.gamete,
                    getIncrementedGameteNonce(),
                    jar,
                    this.chainId,
                    "20000",
                    "1",
                    nonVoidMethodSignature,
                    listOf()
                )
            )
        }

        assertEquals("[how, are, hello, you, ?]", toString?.value)
    }


    @Test fun runInstanceMethodCallTransaction() {
        val toString: StorageValueModel?

        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            val nonVoidMethodSignature = MethodSignatureModel(
                "nonce",
                "java.math.BigInteger",
                listOf(),
                "io.takamaka.code.lang.Account"
            )

            toString = service.runInstanceMethodCallTransaction(
                InstanceMethodCallTransactionRequestModel(
                    "",
                    this.gamete,
                    getIncrementedGameteNonce(),
                    service.getTakamakaCode(),
                    this.chainId,
                    "20000",
                    "1",
                    nonVoidMethodSignature,
                    listOf(),
                    this.gamete
                )
            )
        }

        assertNotNull(toString)
        assertNotNull(toString?.value)
        val integerNonce = Integer.parseInt(toString?.value!!)
        assertTrue(integerNonce > 0)
    }

    @Test fun createFreeAccount() {
        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            try {
                service.addRedGreenGameteCreationTransaction(
                    RedGreenGameteCreationTransactionRequestModel(
                        "10000",
                        "10000",
                        "",
                        service.getTakamakaCode()
                    )
                )
            } catch (e: TransactionRejectedException) {
                assertTrue(e.message!!.equals("cannot run a RedGreenGameteCreationTransactionRequest in an already initialized node"))
                return
            }

            fail("expected exception")
        }
    }

    @Test fun stompClient() {

        val completableFuture = CompletableFuture<Boolean>()
        val stompClient = StompClient("$url/node")
        stompClient.use { client ->

            client.connect(
                {

                    CompletableFuture.runAsync {

                        // subscribe
                        client.subscribeTo("/topic/events", EventRequestModel::class.java, { result, error ->

                            when {
                                error != null -> {
                                    fail("unexpected error")
                                }
                                result != null -> {
                                    val result = eventModel.event.transaction.hash == result.event.transaction.hash &&
                                            eventModel.creator.transaction.hash == result.creator.transaction.hash

                                    completableFuture.complete(result)
                                }
                                else -> {
                                    fail("unexpected payload")
                                }
                            }

                        }, {

                            // send message
                            client.sendTo("/events", eventModel)
                        })
                    }

                }, {
                    fail("Connection failed")
                }
            )

            assertTrue(completableFuture.get(4L, TimeUnit.SECONDS))
        }
    }


    @Test fun events() {
        val completableFuture = CompletableFuture<Boolean>()

        val nodeService : RemoteNode = RemoteNodeClient(url)
        nodeService.use { nodeService_ ->

            val delayedTask = CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS)
            CompletableFuture.runAsync {
                nodeService_.subscribeToEvents(null) { event, key ->
                    val result = eventModel.event.transaction.hash == event.transaction.hash &&
                            eventModel.creator.transaction.hash == key.transaction.hash

                    completableFuture.complete(result)
                }
            }.thenRunAsync(
                {
                    // simulate an EVENT
                    val stompClient = StompClient("$url/node")
                    stompClient.connect({
                        stompClient.sendTo("/events", eventModel)
                    })
                },
                delayedTask
            )

            assertTrue(completableFuture.get(4L, TimeUnit.SECONDS))
        }
    }



    private fun getIncrementedGameteNonce(): String {
        val result: StorageValueModel?

        val nodeService  = RemoteNodeClient(url)
        nodeService.use { service ->

            val nonVoidMethodSignature = MethodSignatureModel(
                "nonce",
                "java.math.BigInteger",
                listOf(),
                "io.takamaka.code.lang.Account"
            )

            result = service.runInstanceMethodCallTransaction(
                InstanceMethodCallTransactionRequestModel(
                    "",
                    this.gamete,
                    "3",
                    service.getTakamakaCode(),
                    this.chainId,
                    "20000",
                    "1",
                    nonVoidMethodSignature,
                    listOf(),
                    this.gamete
                )
            )
        }

        return if (result != null) "" + (Integer.parseInt(result.value!!) + 1) else "0"
    }

    private fun getJarExampleOf(name: String): String {
        return Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get("../../io-takamaka-examples/target/io-takamaka-examples-${takamakaJarVersion}-${name}.jar")))
    }

    private fun getJarTestOf(name: String): String {
        return Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get("../../io-takamaka-code-tests/jars/${name}.jar")))
    }
}