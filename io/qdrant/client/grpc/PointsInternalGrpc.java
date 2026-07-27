package io.qdrant.client.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.65.1)",
    comments = "Source: points_internal_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class PointsInternalGrpc {

  private PointsInternalGrpc() {}

  public static final java.lang.String SERVICE_NAME = "qdrant.PointsInternal";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getUpsertMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Upsert",
      requestType = io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getUpsertMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getUpsertMethod;
    if ((getUpsertMethod = PointsInternalGrpc.getUpsertMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getUpsertMethod = PointsInternalGrpc.getUpsertMethod) == null) {
          PointsInternalGrpc.getUpsertMethod = getUpsertMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Upsert"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("Upsert"))
              .build();
        }
      }
    }
    return getUpsertMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getSyncMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Sync",
      requestType = io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getSyncMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getSyncMethod;
    if ((getSyncMethod = PointsInternalGrpc.getSyncMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getSyncMethod = PointsInternalGrpc.getSyncMethod) == null) {
          PointsInternalGrpc.getSyncMethod = getSyncMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Sync"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("Sync"))
              .build();
        }
      }
    }
    return getSyncMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeleteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Delete",
      requestType = io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeleteMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeleteMethod;
    if ((getDeleteMethod = PointsInternalGrpc.getDeleteMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getDeleteMethod = PointsInternalGrpc.getDeleteMethod) == null) {
          PointsInternalGrpc.getDeleteMethod = getDeleteMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Delete"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("Delete"))
              .build();
        }
      }
    }
    return getDeleteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getUpdateVectorsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateVectors",
      requestType = io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getUpdateVectorsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getUpdateVectorsMethod;
    if ((getUpdateVectorsMethod = PointsInternalGrpc.getUpdateVectorsMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getUpdateVectorsMethod = PointsInternalGrpc.getUpdateVectorsMethod) == null) {
          PointsInternalGrpc.getUpdateVectorsMethod = getUpdateVectorsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateVectors"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("UpdateVectors"))
              .build();
        }
      }
    }
    return getUpdateVectorsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeleteVectorsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteVectors",
      requestType = io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeleteVectorsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeleteVectorsMethod;
    if ((getDeleteVectorsMethod = PointsInternalGrpc.getDeleteVectorsMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getDeleteVectorsMethod = PointsInternalGrpc.getDeleteVectorsMethod) == null) {
          PointsInternalGrpc.getDeleteVectorsMethod = getDeleteVectorsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteVectors"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("DeleteVectors"))
              .build();
        }
      }
    }
    return getDeleteVectorsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getSetPayloadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetPayload",
      requestType = io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getSetPayloadMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getSetPayloadMethod;
    if ((getSetPayloadMethod = PointsInternalGrpc.getSetPayloadMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getSetPayloadMethod = PointsInternalGrpc.getSetPayloadMethod) == null) {
          PointsInternalGrpc.getSetPayloadMethod = getSetPayloadMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetPayload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("SetPayload"))
              .build();
        }
      }
    }
    return getSetPayloadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getOverwritePayloadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OverwritePayload",
      requestType = io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getOverwritePayloadMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getOverwritePayloadMethod;
    if ((getOverwritePayloadMethod = PointsInternalGrpc.getOverwritePayloadMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getOverwritePayloadMethod = PointsInternalGrpc.getOverwritePayloadMethod) == null) {
          PointsInternalGrpc.getOverwritePayloadMethod = getOverwritePayloadMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OverwritePayload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("OverwritePayload"))
              .build();
        }
      }
    }
    return getOverwritePayloadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeletePayloadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeletePayload",
      requestType = io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeletePayloadMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeletePayloadMethod;
    if ((getDeletePayloadMethod = PointsInternalGrpc.getDeletePayloadMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getDeletePayloadMethod = PointsInternalGrpc.getDeletePayloadMethod) == null) {
          PointsInternalGrpc.getDeletePayloadMethod = getDeletePayloadMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeletePayload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("DeletePayload"))
              .build();
        }
      }
    }
    return getDeletePayloadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getClearPayloadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ClearPayload",
      requestType = io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getClearPayloadMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getClearPayloadMethod;
    if ((getClearPayloadMethod = PointsInternalGrpc.getClearPayloadMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getClearPayloadMethod = PointsInternalGrpc.getClearPayloadMethod) == null) {
          PointsInternalGrpc.getClearPayloadMethod = getClearPayloadMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ClearPayload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("ClearPayload"))
              .build();
        }
      }
    }
    return getClearPayloadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getCreateFieldIndexMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateFieldIndex",
      requestType = io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getCreateFieldIndexMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getCreateFieldIndexMethod;
    if ((getCreateFieldIndexMethod = PointsInternalGrpc.getCreateFieldIndexMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getCreateFieldIndexMethod = PointsInternalGrpc.getCreateFieldIndexMethod) == null) {
          PointsInternalGrpc.getCreateFieldIndexMethod = getCreateFieldIndexMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateFieldIndex"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("CreateFieldIndex"))
              .build();
        }
      }
    }
    return getCreateFieldIndexMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeleteFieldIndexMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteFieldIndex",
      requestType = io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal,
      io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeleteFieldIndexMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> getDeleteFieldIndexMethod;
    if ((getDeleteFieldIndexMethod = PointsInternalGrpc.getDeleteFieldIndexMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getDeleteFieldIndexMethod = PointsInternalGrpc.getDeleteFieldIndexMethod) == null) {
          PointsInternalGrpc.getDeleteFieldIndexMethod = getDeleteFieldIndexMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal, io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteFieldIndex"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("DeleteFieldIndex"))
              .build();
        }
      }
    }
    return getDeleteFieldIndexMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal,
      io.qdrant.client.grpc.Points.SearchBatchResponse> getCoreSearchBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CoreSearchBatch",
      requestType = io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal.class,
      responseType = io.qdrant.client.grpc.Points.SearchBatchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal,
      io.qdrant.client.grpc.Points.SearchBatchResponse> getCoreSearchBatchMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal, io.qdrant.client.grpc.Points.SearchBatchResponse> getCoreSearchBatchMethod;
    if ((getCoreSearchBatchMethod = PointsInternalGrpc.getCoreSearchBatchMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getCoreSearchBatchMethod = PointsInternalGrpc.getCoreSearchBatchMethod) == null) {
          PointsInternalGrpc.getCoreSearchBatchMethod = getCoreSearchBatchMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal, io.qdrant.client.grpc.Points.SearchBatchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CoreSearchBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.SearchBatchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("CoreSearchBatch"))
              .build();
        }
      }
    }
    return getCoreSearchBatchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal,
      io.qdrant.client.grpc.Points.ScrollResponse> getScrollMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Scroll",
      requestType = io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal.class,
      responseType = io.qdrant.client.grpc.Points.ScrollResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal,
      io.qdrant.client.grpc.Points.ScrollResponse> getScrollMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal, io.qdrant.client.grpc.Points.ScrollResponse> getScrollMethod;
    if ((getScrollMethod = PointsInternalGrpc.getScrollMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getScrollMethod = PointsInternalGrpc.getScrollMethod) == null) {
          PointsInternalGrpc.getScrollMethod = getScrollMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal, io.qdrant.client.grpc.Points.ScrollResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Scroll"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.ScrollResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("Scroll"))
              .build();
        }
      }
    }
    return getScrollMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.CountPointsInternal,
      io.qdrant.client.grpc.Points.CountResponse> getCountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Count",
      requestType = io.qdrant.client.grpc.PointsInternalService.CountPointsInternal.class,
      responseType = io.qdrant.client.grpc.Points.CountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.CountPointsInternal,
      io.qdrant.client.grpc.Points.CountResponse> getCountMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.CountPointsInternal, io.qdrant.client.grpc.Points.CountResponse> getCountMethod;
    if ((getCountMethod = PointsInternalGrpc.getCountMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getCountMethod = PointsInternalGrpc.getCountMethod) == null) {
          PointsInternalGrpc.getCountMethod = getCountMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.CountPointsInternal, io.qdrant.client.grpc.Points.CountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Count"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.CountPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.CountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("Count"))
              .build();
        }
      }
    }
    return getCountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal,
      io.qdrant.client.grpc.Points.RecommendResponse> getRecommendMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Recommend",
      requestType = io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal.class,
      responseType = io.qdrant.client.grpc.Points.RecommendResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal,
      io.qdrant.client.grpc.Points.RecommendResponse> getRecommendMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal, io.qdrant.client.grpc.Points.RecommendResponse> getRecommendMethod;
    if ((getRecommendMethod = PointsInternalGrpc.getRecommendMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getRecommendMethod = PointsInternalGrpc.getRecommendMethod) == null) {
          PointsInternalGrpc.getRecommendMethod = getRecommendMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal, io.qdrant.client.grpc.Points.RecommendResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Recommend"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.RecommendResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("Recommend"))
              .build();
        }
      }
    }
    return getRecommendMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.GetPointsInternal,
      io.qdrant.client.grpc.Points.GetResponse> getGetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Get",
      requestType = io.qdrant.client.grpc.PointsInternalService.GetPointsInternal.class,
      responseType = io.qdrant.client.grpc.Points.GetResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.GetPointsInternal,
      io.qdrant.client.grpc.Points.GetResponse> getGetMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.GetPointsInternal, io.qdrant.client.grpc.Points.GetResponse> getGetMethod;
    if ((getGetMethod = PointsInternalGrpc.getGetMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getGetMethod = PointsInternalGrpc.getGetMethod) == null) {
          PointsInternalGrpc.getGetMethod = getGetMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.GetPointsInternal, io.qdrant.client.grpc.Points.GetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Get"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.GetPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Points.GetResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("Get"))
              .build();
        }
      }
    }
    return getGetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal> getQueryBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "QueryBatch",
      requestType = io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal,
      io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal> getQueryBatchMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal, io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal> getQueryBatchMethod;
    if ((getQueryBatchMethod = PointsInternalGrpc.getQueryBatchMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getQueryBatchMethod = PointsInternalGrpc.getQueryBatchMethod) == null) {
          PointsInternalGrpc.getQueryBatchMethod = getQueryBatchMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal, io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "QueryBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("QueryBatch"))
              .build();
        }
      }
    }
    return getQueryBatchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal,
      io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal> getFacetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Facet",
      requestType = io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal.class,
      responseType = io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal,
      io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal> getFacetMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal, io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal> getFacetMethod;
    if ((getFacetMethod = PointsInternalGrpc.getFacetMethod) == null) {
      synchronized (PointsInternalGrpc.class) {
        if ((getFacetMethod = PointsInternalGrpc.getFacetMethod) == null) {
          PointsInternalGrpc.getFacetMethod = getFacetMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal, io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Facet"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal.getDefaultInstance()))
              .setSchemaDescriptor(new PointsInternalMethodDescriptorSupplier("Facet"))
              .build();
        }
      }
    }
    return getFacetMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PointsInternalStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PointsInternalStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PointsInternalStub>() {
        @java.lang.Override
        public PointsInternalStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PointsInternalStub(channel, callOptions);
        }
      };
    return PointsInternalStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PointsInternalBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PointsInternalBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PointsInternalBlockingStub>() {
        @java.lang.Override
        public PointsInternalBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PointsInternalBlockingStub(channel, callOptions);
        }
      };
    return PointsInternalBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PointsInternalFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PointsInternalFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PointsInternalFutureStub>() {
        @java.lang.Override
        public PointsInternalFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PointsInternalFutureStub(channel, callOptions);
        }
      };
    return PointsInternalFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void upsert(io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpsertMethod(), responseObserver);
    }

    /**
     */
    default void sync(io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSyncMethod(), responseObserver);
    }

    /**
     */
    default void delete(io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMethod(), responseObserver);
    }

    /**
     */
    default void updateVectors(io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateVectorsMethod(), responseObserver);
    }

    /**
     */
    default void deleteVectors(io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteVectorsMethod(), responseObserver);
    }

    /**
     */
    default void setPayload(io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetPayloadMethod(), responseObserver);
    }

    /**
     */
    default void overwritePayload(io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getOverwritePayloadMethod(), responseObserver);
    }

    /**
     */
    default void deletePayload(io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeletePayloadMethod(), responseObserver);
    }

    /**
     */
    default void clearPayload(io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getClearPayloadMethod(), responseObserver);
    }

    /**
     */
    default void createFieldIndex(io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateFieldIndexMethod(), responseObserver);
    }

    /**
     */
    default void deleteFieldIndex(io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteFieldIndexMethod(), responseObserver);
    }

    /**
     */
    default void coreSearchBatch(io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchBatchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCoreSearchBatchMethod(), responseObserver);
    }

    /**
     */
    default void scroll(io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.ScrollResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getScrollMethod(), responseObserver);
    }

    /**
     */
    default void count(io.qdrant.client.grpc.PointsInternalService.CountPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.CountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCountMethod(), responseObserver);
    }

    /**
     */
    default void recommend(io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRecommendMethod(), responseObserver);
    }

    /**
     */
    default void get(io.qdrant.client.grpc.PointsInternalService.GetPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.GetResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMethod(), responseObserver);
    }

    /**
     */
    default void queryBatch(io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getQueryBatchMethod(), responseObserver);
    }

    /**
     */
    default void facet(io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFacetMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service PointsInternal.
   */
  public static abstract class PointsInternalImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return PointsInternalGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service PointsInternal.
   */
  public static final class PointsInternalStub
      extends io.grpc.stub.AbstractAsyncStub<PointsInternalStub> {
    private PointsInternalStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PointsInternalStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PointsInternalStub(channel, callOptions);
    }

    /**
     */
    public void upsert(io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpsertMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sync(io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSyncMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void delete(io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void updateVectors(io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateVectorsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteVectors(io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteVectorsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setPayload(io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetPayloadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void overwritePayload(io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getOverwritePayloadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deletePayload(io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeletePayloadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void clearPayload(io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getClearPayloadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void createFieldIndex(io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateFieldIndexMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteFieldIndex(io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteFieldIndexMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void coreSearchBatch(io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchBatchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCoreSearchBatchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void scroll(io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.ScrollResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getScrollMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void count(io.qdrant.client.grpc.PointsInternalService.CountPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.CountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void recommend(io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRecommendMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void get(io.qdrant.client.grpc.PointsInternalService.GetPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.GetResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void queryBatch(io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getQueryBatchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void facet(io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFacetMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service PointsInternal.
   */
  public static final class PointsInternalBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<PointsInternalBlockingStub> {
    private PointsInternalBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PointsInternalBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PointsInternalBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal upsert(io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpsertMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal sync(io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSyncMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal delete(io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal updateVectors(io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateVectorsMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal deleteVectors(io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteVectorsMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal setPayload(io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetPayloadMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal overwritePayload(io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getOverwritePayloadMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal deletePayload(io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeletePayloadMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal clearPayload(io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getClearPayloadMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal createFieldIndex(io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateFieldIndexMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal deleteFieldIndex(io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteFieldIndexMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.Points.SearchBatchResponse coreSearchBatch(io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCoreSearchBatchMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.Points.ScrollResponse scroll(io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getScrollMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.Points.CountResponse count(io.qdrant.client.grpc.PointsInternalService.CountPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCountMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.Points.RecommendResponse recommend(io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRecommendMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.Points.GetResponse get(io.qdrant.client.grpc.PointsInternalService.GetPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal queryBatch(io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getQueryBatchMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal facet(io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFacetMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service PointsInternal.
   */
  public static final class PointsInternalFutureStub
      extends io.grpc.stub.AbstractFutureStub<PointsInternalFutureStub> {
    private PointsInternalFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PointsInternalFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PointsInternalFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> upsert(
        io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpsertMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> sync(
        io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSyncMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> delete(
        io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> updateVectors(
        io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateVectorsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> deleteVectors(
        io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteVectorsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> setPayload(
        io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetPayloadMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> overwritePayload(
        io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getOverwritePayloadMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> deletePayload(
        io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeletePayloadMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> clearPayload(
        io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getClearPayloadMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> createFieldIndex(
        io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateFieldIndexMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal> deleteFieldIndex(
        io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteFieldIndexMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.SearchBatchResponse> coreSearchBatch(
        io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCoreSearchBatchMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.ScrollResponse> scroll(
        io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getScrollMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.CountResponse> count(
        io.qdrant.client.grpc.PointsInternalService.CountPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.RecommendResponse> recommend(
        io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRecommendMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Points.GetResponse> get(
        io.qdrant.client.grpc.PointsInternalService.GetPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal> queryBatch(
        io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getQueryBatchMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal> facet(
        io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFacetMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_UPSERT = 0;
  private static final int METHODID_SYNC = 1;
  private static final int METHODID_DELETE = 2;
  private static final int METHODID_UPDATE_VECTORS = 3;
  private static final int METHODID_DELETE_VECTORS = 4;
  private static final int METHODID_SET_PAYLOAD = 5;
  private static final int METHODID_OVERWRITE_PAYLOAD = 6;
  private static final int METHODID_DELETE_PAYLOAD = 7;
  private static final int METHODID_CLEAR_PAYLOAD = 8;
  private static final int METHODID_CREATE_FIELD_INDEX = 9;
  private static final int METHODID_DELETE_FIELD_INDEX = 10;
  private static final int METHODID_CORE_SEARCH_BATCH = 11;
  private static final int METHODID_SCROLL = 12;
  private static final int METHODID_COUNT = 13;
  private static final int METHODID_RECOMMEND = 14;
  private static final int METHODID_GET = 15;
  private static final int METHODID_QUERY_BATCH = 16;
  private static final int METHODID_FACET = 17;

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
        case METHODID_UPSERT:
          serviceImpl.upsert((io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_SYNC:
          serviceImpl.sync((io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_DELETE:
          serviceImpl.delete((io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_UPDATE_VECTORS:
          serviceImpl.updateVectors((io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_DELETE_VECTORS:
          serviceImpl.deleteVectors((io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_SET_PAYLOAD:
          serviceImpl.setPayload((io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_OVERWRITE_PAYLOAD:
          serviceImpl.overwritePayload((io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_DELETE_PAYLOAD:
          serviceImpl.deletePayload((io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_CLEAR_PAYLOAD:
          serviceImpl.clearPayload((io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_CREATE_FIELD_INDEX:
          serviceImpl.createFieldIndex((io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_DELETE_FIELD_INDEX:
          serviceImpl.deleteFieldIndex((io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>) responseObserver);
          break;
        case METHODID_CORE_SEARCH_BATCH:
          serviceImpl.coreSearchBatch((io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.SearchBatchResponse>) responseObserver);
          break;
        case METHODID_SCROLL:
          serviceImpl.scroll((io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.ScrollResponse>) responseObserver);
          break;
        case METHODID_COUNT:
          serviceImpl.count((io.qdrant.client.grpc.PointsInternalService.CountPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.CountResponse>) responseObserver);
          break;
        case METHODID_RECOMMEND:
          serviceImpl.recommend((io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.RecommendResponse>) responseObserver);
          break;
        case METHODID_GET:
          serviceImpl.get((io.qdrant.client.grpc.PointsInternalService.GetPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Points.GetResponse>) responseObserver);
          break;
        case METHODID_QUERY_BATCH:
          serviceImpl.queryBatch((io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal>) responseObserver);
          break;
        case METHODID_FACET:
          serviceImpl.facet((io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal>) responseObserver);
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
          getUpsertMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.UpsertPointsInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_UPSERT)))
        .addMethod(
          getSyncMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.SyncPointsInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_SYNC)))
        .addMethod(
          getDeleteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.DeletePointsInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_DELETE)))
        .addMethod(
          getUpdateVectorsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.UpdateVectorsInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_UPDATE_VECTORS)))
        .addMethod(
          getDeleteVectorsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.DeleteVectorsInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_DELETE_VECTORS)))
        .addMethod(
          getSetPayloadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_SET_PAYLOAD)))
        .addMethod(
          getOverwritePayloadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.SetPayloadPointsInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_OVERWRITE_PAYLOAD)))
        .addMethod(
          getDeletePayloadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.DeletePayloadPointsInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_DELETE_PAYLOAD)))
        .addMethod(
          getClearPayloadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.ClearPayloadPointsInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_CLEAR_PAYLOAD)))
        .addMethod(
          getCreateFieldIndexMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.CreateFieldIndexCollectionInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_CREATE_FIELD_INDEX)))
        .addMethod(
          getDeleteFieldIndexMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.DeleteFieldIndexCollectionInternal,
              io.qdrant.client.grpc.PointsInternalService.PointsOperationResponseInternal>(
                service, METHODID_DELETE_FIELD_INDEX)))
        .addMethod(
          getCoreSearchBatchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.CoreSearchBatchPointsInternal,
              io.qdrant.client.grpc.Points.SearchBatchResponse>(
                service, METHODID_CORE_SEARCH_BATCH)))
        .addMethod(
          getScrollMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.ScrollPointsInternal,
              io.qdrant.client.grpc.Points.ScrollResponse>(
                service, METHODID_SCROLL)))
        .addMethod(
          getCountMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.CountPointsInternal,
              io.qdrant.client.grpc.Points.CountResponse>(
                service, METHODID_COUNT)))
        .addMethod(
          getRecommendMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.RecommendPointsInternal,
              io.qdrant.client.grpc.Points.RecommendResponse>(
                service, METHODID_RECOMMEND)))
        .addMethod(
          getGetMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.GetPointsInternal,
              io.qdrant.client.grpc.Points.GetResponse>(
                service, METHODID_GET)))
        .addMethod(
          getQueryBatchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.QueryBatchPointsInternal,
              io.qdrant.client.grpc.PointsInternalService.QueryBatchResponseInternal>(
                service, METHODID_QUERY_BATCH)))
        .addMethod(
          getFacetMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.PointsInternalService.FacetCountsInternal,
              io.qdrant.client.grpc.PointsInternalService.FacetResponseInternal>(
                service, METHODID_FACET)))
        .build();
  }

  private static abstract class PointsInternalBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PointsInternalBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.qdrant.client.grpc.PointsInternalService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PointsInternal");
    }
  }

  private static final class PointsInternalFileDescriptorSupplier
      extends PointsInternalBaseDescriptorSupplier {
    PointsInternalFileDescriptorSupplier() {}
  }

  private static final class PointsInternalMethodDescriptorSupplier
      extends PointsInternalBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    PointsInternalMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (PointsInternalGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PointsInternalFileDescriptorSupplier())
              .addMethod(getUpsertMethod())
              .addMethod(getSyncMethod())
              .addMethod(getDeleteMethod())
              .addMethod(getUpdateVectorsMethod())
              .addMethod(getDeleteVectorsMethod())
              .addMethod(getSetPayloadMethod())
              .addMethod(getOverwritePayloadMethod())
              .addMethod(getDeletePayloadMethod())
              .addMethod(getClearPayloadMethod())
              .addMethod(getCreateFieldIndexMethod())
              .addMethod(getDeleteFieldIndexMethod())
              .addMethod(getCoreSearchBatchMethod())
              .addMethod(getScrollMethod())
              .addMethod(getCountMethod())
              .addMethod(getRecommendMethod())
              .addMethod(getGetMethod())
              .addMethod(getQueryBatchMethod())
              .addMethod(getFacetMethod())
              .build();
        }
      }
    }
    return result;
  }
}
