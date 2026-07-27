package io.qdrant.client.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.65.1)",
    comments = "Source: shard_snapshots_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ShardSnapshotsGrpc {

  private ShardSnapshotsGrpc() {}

  public static final java.lang.String SERVICE_NAME = "qdrant.ShardSnapshots";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> getCreateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Create",
      requestType = io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest.class,
      responseType = io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> getCreateMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> getCreateMethod;
    if ((getCreateMethod = ShardSnapshotsGrpc.getCreateMethod) == null) {
      synchronized (ShardSnapshotsGrpc.class) {
        if ((getCreateMethod = ShardSnapshotsGrpc.getCreateMethod) == null) {
          ShardSnapshotsGrpc.getCreateMethod = getCreateMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Create"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ShardSnapshotsMethodDescriptorSupplier("Create"))
              .build();
        }
      }
    }
    return getCreateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest,
      io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> getListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "List",
      requestType = io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest.class,
      responseType = io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest,
      io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> getListMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest, io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> getListMethod;
    if ((getListMethod = ShardSnapshotsGrpc.getListMethod) == null) {
      synchronized (ShardSnapshotsGrpc.class) {
        if ((getListMethod = ShardSnapshotsGrpc.getListMethod) == null) {
          ShardSnapshotsGrpc.getListMethod = getListMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest, io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "List"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ShardSnapshotsMethodDescriptorSupplier("List"))
              .build();
        }
      }
    }
    return getListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> getDeleteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Delete",
      requestType = io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest.class,
      responseType = io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest,
      io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> getDeleteMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> getDeleteMethod;
    if ((getDeleteMethod = ShardSnapshotsGrpc.getDeleteMethod) == null) {
      synchronized (ShardSnapshotsGrpc.class) {
        if ((getDeleteMethod = ShardSnapshotsGrpc.getDeleteMethod) == null) {
          ShardSnapshotsGrpc.getDeleteMethod = getDeleteMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest, io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Delete"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ShardSnapshotsMethodDescriptorSupplier("Delete"))
              .build();
        }
      }
    }
    return getDeleteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest,
      io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse> getRecoverMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Recover",
      requestType = io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest.class,
      responseType = io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest,
      io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse> getRecoverMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest, io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse> getRecoverMethod;
    if ((getRecoverMethod = ShardSnapshotsGrpc.getRecoverMethod) == null) {
      synchronized (ShardSnapshotsGrpc.class) {
        if ((getRecoverMethod = ShardSnapshotsGrpc.getRecoverMethod) == null) {
          ShardSnapshotsGrpc.getRecoverMethod = getRecoverMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest, io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Recover"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ShardSnapshotsMethodDescriptorSupplier("Recover"))
              .build();
        }
      }
    }
    return getRecoverMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ShardSnapshotsStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ShardSnapshotsStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ShardSnapshotsStub>() {
        @java.lang.Override
        public ShardSnapshotsStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ShardSnapshotsStub(channel, callOptions);
        }
      };
    return ShardSnapshotsStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ShardSnapshotsBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ShardSnapshotsBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ShardSnapshotsBlockingStub>() {
        @java.lang.Override
        public ShardSnapshotsBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ShardSnapshotsBlockingStub(channel, callOptions);
        }
      };
    return ShardSnapshotsBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ShardSnapshotsFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ShardSnapshotsFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ShardSnapshotsFutureStub>() {
        @java.lang.Override
        public ShardSnapshotsFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ShardSnapshotsFutureStub(channel, callOptions);
        }
      };
    return ShardSnapshotsFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     *Create shard snapshot
     * </pre>
     */
    default void create(io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateMethod(), responseObserver);
    }

    /**
     * <pre>
     *List shard snapshots
     * </pre>
     */
    default void list(io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListMethod(), responseObserver);
    }

    /**
     * <pre>
     *Delete shard snapshot
     * </pre>
     */
    default void delete(io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMethod(), responseObserver);
    }

    /**
     * <pre>
     *Recover shard snapshot
     * </pre>
     */
    default void recover(io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRecoverMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ShardSnapshots.
   */
  public static abstract class ShardSnapshotsImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ShardSnapshotsGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ShardSnapshots.
   */
  public static final class ShardSnapshotsStub
      extends io.grpc.stub.AbstractAsyncStub<ShardSnapshotsStub> {
    private ShardSnapshotsStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ShardSnapshotsStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ShardSnapshotsStub(channel, callOptions);
    }

    /**
     * <pre>
     *Create shard snapshot
     * </pre>
     */
    public void create(io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *List shard snapshots
     * </pre>
     */
    public void list(io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Delete shard snapshot
     * </pre>
     */
    public void delete(io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Recover shard snapshot
     * </pre>
     */
    public void recover(io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRecoverMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ShardSnapshots.
   */
  public static final class ShardSnapshotsBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ShardSnapshotsBlockingStub> {
    private ShardSnapshotsBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ShardSnapshotsBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ShardSnapshotsBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     *Create shard snapshot
     * </pre>
     */
    public io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse create(io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *List shard snapshots
     * </pre>
     */
    public io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse list(io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Delete shard snapshot
     * </pre>
     */
    public io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse delete(io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Recover shard snapshot
     * </pre>
     */
    public io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse recover(io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRecoverMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ShardSnapshots.
   */
  public static final class ShardSnapshotsFutureStub
      extends io.grpc.stub.AbstractFutureStub<ShardSnapshotsFutureStub> {
    private ShardSnapshotsFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ShardSnapshotsFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ShardSnapshotsFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     *Create shard snapshot
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse> create(
        io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *List shard snapshots
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse> list(
        io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Delete shard snapshot
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse> delete(
        io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Recover shard snapshot
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse> recover(
        io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRecoverMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE = 0;
  private static final int METHODID_LIST = 1;
  private static final int METHODID_DELETE = 2;
  private static final int METHODID_RECOVER = 3;

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
        case METHODID_CREATE:
          serviceImpl.create((io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse>) responseObserver);
          break;
        case METHODID_LIST:
          serviceImpl.list((io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse>) responseObserver);
          break;
        case METHODID_DELETE:
          serviceImpl.delete((io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse>) responseObserver);
          break;
        case METHODID_RECOVER:
          serviceImpl.recover((io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse>) responseObserver);
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
          getCreateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.ShardSnapshotsService.CreateShardSnapshotRequest,
              io.qdrant.client.grpc.SnapshotsService.CreateSnapshotResponse>(
                service, METHODID_CREATE)))
        .addMethod(
          getListMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.ShardSnapshotsService.ListShardSnapshotsRequest,
              io.qdrant.client.grpc.SnapshotsService.ListSnapshotsResponse>(
                service, METHODID_LIST)))
        .addMethod(
          getDeleteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.ShardSnapshotsService.DeleteShardSnapshotRequest,
              io.qdrant.client.grpc.SnapshotsService.DeleteSnapshotResponse>(
                service, METHODID_DELETE)))
        .addMethod(
          getRecoverMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.ShardSnapshotsService.RecoverShardSnapshotRequest,
              io.qdrant.client.grpc.ShardSnapshotsService.RecoverSnapshotResponse>(
                service, METHODID_RECOVER)))
        .build();
  }

  private static abstract class ShardSnapshotsBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ShardSnapshotsBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.qdrant.client.grpc.ShardSnapshotsService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ShardSnapshots");
    }
  }

  private static final class ShardSnapshotsFileDescriptorSupplier
      extends ShardSnapshotsBaseDescriptorSupplier {
    ShardSnapshotsFileDescriptorSupplier() {}
  }

  private static final class ShardSnapshotsMethodDescriptorSupplier
      extends ShardSnapshotsBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ShardSnapshotsMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (ShardSnapshotsGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ShardSnapshotsFileDescriptorSupplier())
              .addMethod(getCreateMethod())
              .addMethod(getListMethod())
              .addMethod(getDeleteMethod())
              .addMethod(getRecoverMethod())
              .build();
        }
      }
    }
    return result;
  }
}
