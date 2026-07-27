package io.qdrant.client.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.65.1)",
    comments = "Source: collections_internal_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class CollectionsInternalGrpc {

  private CollectionsInternalGrpc() {}

  public static final java.lang.String SERVICE_NAME = "qdrant.CollectionsInternal";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal,
      io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> getGetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Get",
      requestType = io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal.class,
      responseType = io.qdrant.client.grpc.Collections.GetCollectionInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal,
      io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> getGetMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal, io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> getGetMethod;
    if ((getGetMethod = CollectionsInternalGrpc.getGetMethod) == null) {
      synchronized (CollectionsInternalGrpc.class) {
        if ((getGetMethod = CollectionsInternalGrpc.getGetMethod) == null) {
          CollectionsInternalGrpc.getGetMethod = getGetMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal, io.qdrant.client.grpc.Collections.GetCollectionInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Get"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.GetCollectionInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsInternalMethodDescriptorSupplier("Get"))
              .build();
        }
      }
    }
    return getGetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getInitiateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Initiate",
      requestType = io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest.class,
      responseType = io.qdrant.client.grpc.Collections.CollectionOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getInitiateMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest, io.qdrant.client.grpc.Collections.CollectionOperationResponse> getInitiateMethod;
    if ((getInitiateMethod = CollectionsInternalGrpc.getInitiateMethod) == null) {
      synchronized (CollectionsInternalGrpc.class) {
        if ((getInitiateMethod = CollectionsInternalGrpc.getInitiateMethod) == null) {
          CollectionsInternalGrpc.getInitiateMethod = getInitiateMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest, io.qdrant.client.grpc.Collections.CollectionOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Initiate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsInternalMethodDescriptorSupplier("Initiate"))
              .build();
        }
      }
    }
    return getInitiateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getWaitForShardStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WaitForShardState",
      requestType = io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest.class,
      responseType = io.qdrant.client.grpc.Collections.CollectionOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getWaitForShardStateMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest, io.qdrant.client.grpc.Collections.CollectionOperationResponse> getWaitForShardStateMethod;
    if ((getWaitForShardStateMethod = CollectionsInternalGrpc.getWaitForShardStateMethod) == null) {
      synchronized (CollectionsInternalGrpc.class) {
        if ((getWaitForShardStateMethod = CollectionsInternalGrpc.getWaitForShardStateMethod) == null) {
          CollectionsInternalGrpc.getWaitForShardStateMethod = getWaitForShardStateMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest, io.qdrant.client.grpc.Collections.CollectionOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WaitForShardState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsInternalMethodDescriptorSupplier("WaitForShardState"))
              .build();
        }
      }
    }
    return getWaitForShardStateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest,
      io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse> getGetShardRecoveryPointMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetShardRecoveryPoint",
      requestType = io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest.class,
      responseType = io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest,
      io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse> getGetShardRecoveryPointMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest, io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse> getGetShardRecoveryPointMethod;
    if ((getGetShardRecoveryPointMethod = CollectionsInternalGrpc.getGetShardRecoveryPointMethod) == null) {
      synchronized (CollectionsInternalGrpc.class) {
        if ((getGetShardRecoveryPointMethod = CollectionsInternalGrpc.getGetShardRecoveryPointMethod) == null) {
          CollectionsInternalGrpc.getGetShardRecoveryPointMethod = getGetShardRecoveryPointMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest, io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetShardRecoveryPoint"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsInternalMethodDescriptorSupplier("GetShardRecoveryPoint"))
              .build();
        }
      }
    }
    return getGetShardRecoveryPointMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getUpdateShardCutoffPointMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateShardCutoffPoint",
      requestType = io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest.class,
      responseType = io.qdrant.client.grpc.Collections.CollectionOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getUpdateShardCutoffPointMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest, io.qdrant.client.grpc.Collections.CollectionOperationResponse> getUpdateShardCutoffPointMethod;
    if ((getUpdateShardCutoffPointMethod = CollectionsInternalGrpc.getUpdateShardCutoffPointMethod) == null) {
      synchronized (CollectionsInternalGrpc.class) {
        if ((getUpdateShardCutoffPointMethod = CollectionsInternalGrpc.getUpdateShardCutoffPointMethod) == null) {
          CollectionsInternalGrpc.getUpdateShardCutoffPointMethod = getUpdateShardCutoffPointMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest, io.qdrant.client.grpc.Collections.CollectionOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateShardCutoffPoint"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsInternalMethodDescriptorSupplier("UpdateShardCutoffPoint"))
              .build();
        }
      }
    }
    return getUpdateShardCutoffPointMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CollectionsInternalStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollectionsInternalStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollectionsInternalStub>() {
        @java.lang.Override
        public CollectionsInternalStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollectionsInternalStub(channel, callOptions);
        }
      };
    return CollectionsInternalStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CollectionsInternalBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollectionsInternalBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollectionsInternalBlockingStub>() {
        @java.lang.Override
        public CollectionsInternalBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollectionsInternalBlockingStub(channel, callOptions);
        }
      };
    return CollectionsInternalBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CollectionsInternalFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollectionsInternalFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollectionsInternalFutureStub>() {
        @java.lang.Override
        public CollectionsInternalFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollectionsInternalFutureStub(channel, callOptions);
        }
      };
    return CollectionsInternalFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     *Get collection info
     * </pre>
     */
    default void get(io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Initiate shard transfer
     * </pre>
     */
    default void initiate(io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getInitiateMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     *Wait for a shard to get into the given state
     * </pre>
     */
    default void waitForShardState(io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getWaitForShardStateMethod(), responseObserver);
    }

    /**
     * <pre>
     *Get shard recovery point
     * </pre>
     */
    default void getShardRecoveryPoint(io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetShardRecoveryPointMethod(), responseObserver);
    }

    /**
     * <pre>
     *Update shard cutoff point
     * </pre>
     */
    default void updateShardCutoffPoint(io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateShardCutoffPointMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service CollectionsInternal.
   */
  public static abstract class CollectionsInternalImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return CollectionsInternalGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service CollectionsInternal.
   */
  public static final class CollectionsInternalStub
      extends io.grpc.stub.AbstractAsyncStub<CollectionsInternalStub> {
    private CollectionsInternalStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollectionsInternalStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollectionsInternalStub(channel, callOptions);
    }

    /**
     * <pre>
     *Get collection info
     * </pre>
     */
    public void get(io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Initiate shard transfer
     * </pre>
     */
    public void initiate(io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getInitiateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     *Wait for a shard to get into the given state
     * </pre>
     */
    public void waitForShardState(io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getWaitForShardStateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Get shard recovery point
     * </pre>
     */
    public void getShardRecoveryPoint(io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetShardRecoveryPointMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Update shard cutoff point
     * </pre>
     */
    public void updateShardCutoffPoint(io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateShardCutoffPointMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service CollectionsInternal.
   */
  public static final class CollectionsInternalBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<CollectionsInternalBlockingStub> {
    private CollectionsInternalBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollectionsInternalBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollectionsInternalBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     *Get collection info
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.GetCollectionInfoResponse get(io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Initiate shard transfer
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.CollectionOperationResponse initiate(io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getInitiateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     *Wait for a shard to get into the given state
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.CollectionOperationResponse waitForShardState(io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getWaitForShardStateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Get shard recovery point
     * </pre>
     */
    public io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse getShardRecoveryPoint(io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetShardRecoveryPointMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Update shard cutoff point
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.CollectionOperationResponse updateShardCutoffPoint(io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateShardCutoffPointMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service CollectionsInternal.
   */
  public static final class CollectionsInternalFutureStub
      extends io.grpc.stub.AbstractFutureStub<CollectionsInternalFutureStub> {
    private CollectionsInternalFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollectionsInternalFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollectionsInternalFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     *Get collection info
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> get(
        io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Initiate shard transfer
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.CollectionOperationResponse> initiate(
        io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getInitiateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     *Wait for a shard to get into the given state
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.CollectionOperationResponse> waitForShardState(
        io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getWaitForShardStateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Get shard recovery point
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse> getShardRecoveryPoint(
        io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetShardRecoveryPointMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Update shard cutoff point
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.CollectionOperationResponse> updateShardCutoffPoint(
        io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateShardCutoffPointMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET = 0;
  private static final int METHODID_INITIATE = 1;
  private static final int METHODID_WAIT_FOR_SHARD_STATE = 2;
  private static final int METHODID_GET_SHARD_RECOVERY_POINT = 3;
  private static final int METHODID_UPDATE_SHARD_CUTOFF_POINT = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET:
          serviceImpl.get((io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.GetCollectionInfoResponse>) responseObserver);
          break;
        case METHODID_INITIATE:
          serviceImpl.initiate((io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse>) responseObserver);
          break;
        case METHODID_WAIT_FOR_SHARD_STATE:
          serviceImpl.waitForShardState((io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse>) responseObserver);
          break;
        case METHODID_GET_SHARD_RECOVERY_POINT:
          serviceImpl.getShardRecoveryPoint((io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse>) responseObserver);
          break;
        case METHODID_UPDATE_SHARD_CUTOFF_POINT:
          serviceImpl.updateShardCutoffPoint((io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.CollectionsInternalService.GetCollectionInfoRequestInternal,
              io.qdrant.client.grpc.Collections.GetCollectionInfoResponse>(
                service, METHODID_GET)))
        .addMethod(
          getInitiateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.CollectionsInternalService.InitiateShardTransferRequest,
              io.qdrant.client.grpc.Collections.CollectionOperationResponse>(
                service, METHODID_INITIATE)))
        .addMethod(
          getWaitForShardStateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.CollectionsInternalService.WaitForShardStateRequest,
              io.qdrant.client.grpc.Collections.CollectionOperationResponse>(
                service, METHODID_WAIT_FOR_SHARD_STATE)))
        .addMethod(
          getGetShardRecoveryPointMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointRequest,
              io.qdrant.client.grpc.CollectionsInternalService.GetShardRecoveryPointResponse>(
                service, METHODID_GET_SHARD_RECOVERY_POINT)))
        .addMethod(
          getUpdateShardCutoffPointMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.CollectionsInternalService.UpdateShardCutoffPointRequest,
              io.qdrant.client.grpc.Collections.CollectionOperationResponse>(
                service, METHODID_UPDATE_SHARD_CUTOFF_POINT)))
        .build();
  }

  private static abstract class CollectionsInternalBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CollectionsInternalBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.qdrant.client.grpc.CollectionsInternalService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("CollectionsInternal");
    }
  }

  private static final class CollectionsInternalFileDescriptorSupplier
      extends CollectionsInternalBaseDescriptorSupplier {
    CollectionsInternalFileDescriptorSupplier() {}
  }

  private static final class CollectionsInternalMethodDescriptorSupplier
      extends CollectionsInternalBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    CollectionsInternalMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (CollectionsInternalGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CollectionsInternalFileDescriptorSupplier())
              .addMethod(getGetMethod())
              .addMethod(getInitiateMethod())
              .addMethod(getWaitForShardStateMethod())
              .addMethod(getGetShardRecoveryPointMethod())
              .addMethod(getUpdateShardCutoffPointMethod())
              .build();
        }
      }
    }
    return result;
  }
}
