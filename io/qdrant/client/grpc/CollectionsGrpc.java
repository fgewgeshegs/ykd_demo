package io.qdrant.client.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.65.1)",
    comments = "Source: collections_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class CollectionsGrpc {

  private CollectionsGrpc() {}

  public static final java.lang.String SERVICE_NAME = "qdrant.Collections";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.GetCollectionInfoRequest,
      io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> getGetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Get",
      requestType = io.qdrant.client.grpc.Collections.GetCollectionInfoRequest.class,
      responseType = io.qdrant.client.grpc.Collections.GetCollectionInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.GetCollectionInfoRequest,
      io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> getGetMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.GetCollectionInfoRequest, io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> getGetMethod;
    if ((getGetMethod = CollectionsGrpc.getGetMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getGetMethod = CollectionsGrpc.getGetMethod) == null) {
          CollectionsGrpc.getGetMethod = getGetMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.GetCollectionInfoRequest, io.qdrant.client.grpc.Collections.GetCollectionInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Get"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.GetCollectionInfoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.GetCollectionInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("Get"))
              .build();
        }
      }
    }
    return getGetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ListCollectionsRequest,
      io.qdrant.client.grpc.Collections.ListCollectionsResponse> getListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "List",
      requestType = io.qdrant.client.grpc.Collections.ListCollectionsRequest.class,
      responseType = io.qdrant.client.grpc.Collections.ListCollectionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ListCollectionsRequest,
      io.qdrant.client.grpc.Collections.ListCollectionsResponse> getListMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ListCollectionsRequest, io.qdrant.client.grpc.Collections.ListCollectionsResponse> getListMethod;
    if ((getListMethod = CollectionsGrpc.getListMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getListMethod = CollectionsGrpc.getListMethod) == null) {
          CollectionsGrpc.getListMethod = getListMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.ListCollectionsRequest, io.qdrant.client.grpc.Collections.ListCollectionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "List"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.ListCollectionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.ListCollectionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("List"))
              .build();
        }
      }
    }
    return getListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CreateCollection,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getCreateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Create",
      requestType = io.qdrant.client.grpc.Collections.CreateCollection.class,
      responseType = io.qdrant.client.grpc.Collections.CollectionOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CreateCollection,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getCreateMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CreateCollection, io.qdrant.client.grpc.Collections.CollectionOperationResponse> getCreateMethod;
    if ((getCreateMethod = CollectionsGrpc.getCreateMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getCreateMethod = CollectionsGrpc.getCreateMethod) == null) {
          CollectionsGrpc.getCreateMethod = getCreateMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.CreateCollection, io.qdrant.client.grpc.Collections.CollectionOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Create"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CreateCollection.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("Create"))
              .build();
        }
      }
    }
    return getCreateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.UpdateCollection,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getUpdateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Update",
      requestType = io.qdrant.client.grpc.Collections.UpdateCollection.class,
      responseType = io.qdrant.client.grpc.Collections.CollectionOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.UpdateCollection,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getUpdateMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.UpdateCollection, io.qdrant.client.grpc.Collections.CollectionOperationResponse> getUpdateMethod;
    if ((getUpdateMethod = CollectionsGrpc.getUpdateMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getUpdateMethod = CollectionsGrpc.getUpdateMethod) == null) {
          CollectionsGrpc.getUpdateMethod = getUpdateMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.UpdateCollection, io.qdrant.client.grpc.Collections.CollectionOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Update"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.UpdateCollection.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("Update"))
              .build();
        }
      }
    }
    return getUpdateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.DeleteCollection,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getDeleteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Delete",
      requestType = io.qdrant.client.grpc.Collections.DeleteCollection.class,
      responseType = io.qdrant.client.grpc.Collections.CollectionOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.DeleteCollection,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getDeleteMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.DeleteCollection, io.qdrant.client.grpc.Collections.CollectionOperationResponse> getDeleteMethod;
    if ((getDeleteMethod = CollectionsGrpc.getDeleteMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getDeleteMethod = CollectionsGrpc.getDeleteMethod) == null) {
          CollectionsGrpc.getDeleteMethod = getDeleteMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.DeleteCollection, io.qdrant.client.grpc.Collections.CollectionOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Delete"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.DeleteCollection.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("Delete"))
              .build();
        }
      }
    }
    return getDeleteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ChangeAliases,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getUpdateAliasesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateAliases",
      requestType = io.qdrant.client.grpc.Collections.ChangeAliases.class,
      responseType = io.qdrant.client.grpc.Collections.CollectionOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ChangeAliases,
      io.qdrant.client.grpc.Collections.CollectionOperationResponse> getUpdateAliasesMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ChangeAliases, io.qdrant.client.grpc.Collections.CollectionOperationResponse> getUpdateAliasesMethod;
    if ((getUpdateAliasesMethod = CollectionsGrpc.getUpdateAliasesMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getUpdateAliasesMethod = CollectionsGrpc.getUpdateAliasesMethod) == null) {
          CollectionsGrpc.getUpdateAliasesMethod = getUpdateAliasesMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.ChangeAliases, io.qdrant.client.grpc.Collections.CollectionOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateAliases"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.ChangeAliases.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("UpdateAliases"))
              .build();
        }
      }
    }
    return getUpdateAliasesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest,
      io.qdrant.client.grpc.Collections.ListAliasesResponse> getListCollectionAliasesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListCollectionAliases",
      requestType = io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest.class,
      responseType = io.qdrant.client.grpc.Collections.ListAliasesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest,
      io.qdrant.client.grpc.Collections.ListAliasesResponse> getListCollectionAliasesMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest, io.qdrant.client.grpc.Collections.ListAliasesResponse> getListCollectionAliasesMethod;
    if ((getListCollectionAliasesMethod = CollectionsGrpc.getListCollectionAliasesMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getListCollectionAliasesMethod = CollectionsGrpc.getListCollectionAliasesMethod) == null) {
          CollectionsGrpc.getListCollectionAliasesMethod = getListCollectionAliasesMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest, io.qdrant.client.grpc.Collections.ListAliasesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListCollectionAliases"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.ListAliasesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("ListCollectionAliases"))
              .build();
        }
      }
    }
    return getListCollectionAliasesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ListAliasesRequest,
      io.qdrant.client.grpc.Collections.ListAliasesResponse> getListAliasesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListAliases",
      requestType = io.qdrant.client.grpc.Collections.ListAliasesRequest.class,
      responseType = io.qdrant.client.grpc.Collections.ListAliasesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ListAliasesRequest,
      io.qdrant.client.grpc.Collections.ListAliasesResponse> getListAliasesMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.ListAliasesRequest, io.qdrant.client.grpc.Collections.ListAliasesResponse> getListAliasesMethod;
    if ((getListAliasesMethod = CollectionsGrpc.getListAliasesMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getListAliasesMethod = CollectionsGrpc.getListAliasesMethod) == null) {
          CollectionsGrpc.getListAliasesMethod = getListAliasesMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.ListAliasesRequest, io.qdrant.client.grpc.Collections.ListAliasesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListAliases"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.ListAliasesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.ListAliasesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("ListAliases"))
              .build();
        }
      }
    }
    return getListAliasesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest,
      io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse> getCollectionClusterInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CollectionClusterInfo",
      requestType = io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest.class,
      responseType = io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest,
      io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse> getCollectionClusterInfoMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest, io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse> getCollectionClusterInfoMethod;
    if ((getCollectionClusterInfoMethod = CollectionsGrpc.getCollectionClusterInfoMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getCollectionClusterInfoMethod = CollectionsGrpc.getCollectionClusterInfoMethod) == null) {
          CollectionsGrpc.getCollectionClusterInfoMethod = getCollectionClusterInfoMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest, io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CollectionClusterInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("CollectionClusterInfo"))
              .build();
        }
      }
    }
    return getCollectionClusterInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CollectionExistsRequest,
      io.qdrant.client.grpc.Collections.CollectionExistsResponse> getCollectionExistsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CollectionExists",
      requestType = io.qdrant.client.grpc.Collections.CollectionExistsRequest.class,
      responseType = io.qdrant.client.grpc.Collections.CollectionExistsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CollectionExistsRequest,
      io.qdrant.client.grpc.Collections.CollectionExistsResponse> getCollectionExistsMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CollectionExistsRequest, io.qdrant.client.grpc.Collections.CollectionExistsResponse> getCollectionExistsMethod;
    if ((getCollectionExistsMethod = CollectionsGrpc.getCollectionExistsMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getCollectionExistsMethod = CollectionsGrpc.getCollectionExistsMethod) == null) {
          CollectionsGrpc.getCollectionExistsMethod = getCollectionExistsMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.CollectionExistsRequest, io.qdrant.client.grpc.Collections.CollectionExistsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CollectionExists"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionExistsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CollectionExistsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("CollectionExists"))
              .build();
        }
      }
    }
    return getCollectionExistsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest,
      io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse> getUpdateCollectionClusterSetupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateCollectionClusterSetup",
      requestType = io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest.class,
      responseType = io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest,
      io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse> getUpdateCollectionClusterSetupMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest, io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse> getUpdateCollectionClusterSetupMethod;
    if ((getUpdateCollectionClusterSetupMethod = CollectionsGrpc.getUpdateCollectionClusterSetupMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getUpdateCollectionClusterSetupMethod = CollectionsGrpc.getUpdateCollectionClusterSetupMethod) == null) {
          CollectionsGrpc.getUpdateCollectionClusterSetupMethod = getUpdateCollectionClusterSetupMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest, io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateCollectionClusterSetup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("UpdateCollectionClusterSetup"))
              .build();
        }
      }
    }
    return getUpdateCollectionClusterSetupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CreateShardKeyRequest,
      io.qdrant.client.grpc.Collections.CreateShardKeyResponse> getCreateShardKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateShardKey",
      requestType = io.qdrant.client.grpc.Collections.CreateShardKeyRequest.class,
      responseType = io.qdrant.client.grpc.Collections.CreateShardKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CreateShardKeyRequest,
      io.qdrant.client.grpc.Collections.CreateShardKeyResponse> getCreateShardKeyMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.CreateShardKeyRequest, io.qdrant.client.grpc.Collections.CreateShardKeyResponse> getCreateShardKeyMethod;
    if ((getCreateShardKeyMethod = CollectionsGrpc.getCreateShardKeyMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getCreateShardKeyMethod = CollectionsGrpc.getCreateShardKeyMethod) == null) {
          CollectionsGrpc.getCreateShardKeyMethod = getCreateShardKeyMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.CreateShardKeyRequest, io.qdrant.client.grpc.Collections.CreateShardKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateShardKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CreateShardKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.CreateShardKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("CreateShardKey"))
              .build();
        }
      }
    }
    return getCreateShardKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.DeleteShardKeyRequest,
      io.qdrant.client.grpc.Collections.DeleteShardKeyResponse> getDeleteShardKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteShardKey",
      requestType = io.qdrant.client.grpc.Collections.DeleteShardKeyRequest.class,
      responseType = io.qdrant.client.grpc.Collections.DeleteShardKeyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.DeleteShardKeyRequest,
      io.qdrant.client.grpc.Collections.DeleteShardKeyResponse> getDeleteShardKeyMethod() {
    io.grpc.MethodDescriptor<io.qdrant.client.grpc.Collections.DeleteShardKeyRequest, io.qdrant.client.grpc.Collections.DeleteShardKeyResponse> getDeleteShardKeyMethod;
    if ((getDeleteShardKeyMethod = CollectionsGrpc.getDeleteShardKeyMethod) == null) {
      synchronized (CollectionsGrpc.class) {
        if ((getDeleteShardKeyMethod = CollectionsGrpc.getDeleteShardKeyMethod) == null) {
          CollectionsGrpc.getDeleteShardKeyMethod = getDeleteShardKeyMethod =
              io.grpc.MethodDescriptor.<io.qdrant.client.grpc.Collections.DeleteShardKeyRequest, io.qdrant.client.grpc.Collections.DeleteShardKeyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteShardKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.DeleteShardKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.qdrant.client.grpc.Collections.DeleteShardKeyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectionsMethodDescriptorSupplier("DeleteShardKey"))
              .build();
        }
      }
    }
    return getDeleteShardKeyMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CollectionsStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollectionsStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollectionsStub>() {
        @java.lang.Override
        public CollectionsStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollectionsStub(channel, callOptions);
        }
      };
    return CollectionsStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CollectionsBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollectionsBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollectionsBlockingStub>() {
        @java.lang.Override
        public CollectionsBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollectionsBlockingStub(channel, callOptions);
        }
      };
    return CollectionsBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CollectionsFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollectionsFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollectionsFutureStub>() {
        @java.lang.Override
        public CollectionsFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollectionsFutureStub(channel, callOptions);
        }
      };
    return CollectionsFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     *Get detailed information about specified existing collection
     * </pre>
     */
    default void get(io.qdrant.client.grpc.Collections.GetCollectionInfoRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Get list name of all existing collections
     * </pre>
     */
    default void list(io.qdrant.client.grpc.Collections.ListCollectionsRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.ListCollectionsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListMethod(), responseObserver);
    }

    /**
     * <pre>
     *Create new collection with given parameters
     * </pre>
     */
    default void create(io.qdrant.client.grpc.Collections.CreateCollection request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateMethod(), responseObserver);
    }

    /**
     * <pre>
     *Update parameters of the existing collection
     * </pre>
     */
    default void update(io.qdrant.client.grpc.Collections.UpdateCollection request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateMethod(), responseObserver);
    }

    /**
     * <pre>
     *Drop collection and all associated data
     * </pre>
     */
    default void delete(io.qdrant.client.grpc.Collections.DeleteCollection request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMethod(), responseObserver);
    }

    /**
     * <pre>
     *Update Aliases of the existing collection
     * </pre>
     */
    default void updateAliases(io.qdrant.client.grpc.Collections.ChangeAliases request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateAliasesMethod(), responseObserver);
    }

    /**
     * <pre>
     *Get list of all aliases for a collection
     * </pre>
     */
    default void listCollectionAliases(io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.ListAliasesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListCollectionAliasesMethod(), responseObserver);
    }

    /**
     * <pre>
     *Get list of all aliases for all existing collections
     * </pre>
     */
    default void listAliases(io.qdrant.client.grpc.Collections.ListAliasesRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.ListAliasesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListAliasesMethod(), responseObserver);
    }

    /**
     * <pre>
     *Get cluster information for a collection
     * </pre>
     */
    default void collectionClusterInfo(io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCollectionClusterInfoMethod(), responseObserver);
    }

    /**
     * <pre>
     *Check the existence of a collection
     * </pre>
     */
    default void collectionExists(io.qdrant.client.grpc.Collections.CollectionExistsRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionExistsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCollectionExistsMethod(), responseObserver);
    }

    /**
     * <pre>
     *Update cluster setup for a collection
     * </pre>
     */
    default void updateCollectionClusterSetup(io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateCollectionClusterSetupMethod(), responseObserver);
    }

    /**
     * <pre>
     *Create shard key
     * </pre>
     */
    default void createShardKey(io.qdrant.client.grpc.Collections.CreateShardKeyRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CreateShardKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateShardKeyMethod(), responseObserver);
    }

    /**
     * <pre>
     *Delete shard key
     * </pre>
     */
    default void deleteShardKey(io.qdrant.client.grpc.Collections.DeleteShardKeyRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.DeleteShardKeyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteShardKeyMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service Collections.
   */
  public static abstract class CollectionsImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return CollectionsGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service Collections.
   */
  public static final class CollectionsStub
      extends io.grpc.stub.AbstractAsyncStub<CollectionsStub> {
    private CollectionsStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollectionsStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollectionsStub(channel, callOptions);
    }

    /**
     * <pre>
     *Get detailed information about specified existing collection
     * </pre>
     */
    public void get(io.qdrant.client.grpc.Collections.GetCollectionInfoRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Get list name of all existing collections
     * </pre>
     */
    public void list(io.qdrant.client.grpc.Collections.ListCollectionsRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.ListCollectionsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Create new collection with given parameters
     * </pre>
     */
    public void create(io.qdrant.client.grpc.Collections.CreateCollection request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Update parameters of the existing collection
     * </pre>
     */
    public void update(io.qdrant.client.grpc.Collections.UpdateCollection request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Drop collection and all associated data
     * </pre>
     */
    public void delete(io.qdrant.client.grpc.Collections.DeleteCollection request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Update Aliases of the existing collection
     * </pre>
     */
    public void updateAliases(io.qdrant.client.grpc.Collections.ChangeAliases request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateAliasesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Get list of all aliases for a collection
     * </pre>
     */
    public void listCollectionAliases(io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.ListAliasesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListCollectionAliasesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Get list of all aliases for all existing collections
     * </pre>
     */
    public void listAliases(io.qdrant.client.grpc.Collections.ListAliasesRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.ListAliasesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListAliasesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Get cluster information for a collection
     * </pre>
     */
    public void collectionClusterInfo(io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCollectionClusterInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Check the existence of a collection
     * </pre>
     */
    public void collectionExists(io.qdrant.client.grpc.Collections.CollectionExistsRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionExistsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCollectionExistsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Update cluster setup for a collection
     * </pre>
     */
    public void updateCollectionClusterSetup(io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateCollectionClusterSetupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Create shard key
     * </pre>
     */
    public void createShardKey(io.qdrant.client.grpc.Collections.CreateShardKeyRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CreateShardKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateShardKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Delete shard key
     * </pre>
     */
    public void deleteShardKey(io.qdrant.client.grpc.Collections.DeleteShardKeyRequest request,
        io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.DeleteShardKeyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteShardKeyMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service Collections.
   */
  public static final class CollectionsBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<CollectionsBlockingStub> {
    private CollectionsBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollectionsBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollectionsBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     *Get detailed information about specified existing collection
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.GetCollectionInfoResponse get(io.qdrant.client.grpc.Collections.GetCollectionInfoRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Get list name of all existing collections
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.ListCollectionsResponse list(io.qdrant.client.grpc.Collections.ListCollectionsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Create new collection with given parameters
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.CollectionOperationResponse create(io.qdrant.client.grpc.Collections.CreateCollection request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Update parameters of the existing collection
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.CollectionOperationResponse update(io.qdrant.client.grpc.Collections.UpdateCollection request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Drop collection and all associated data
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.CollectionOperationResponse delete(io.qdrant.client.grpc.Collections.DeleteCollection request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Update Aliases of the existing collection
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.CollectionOperationResponse updateAliases(io.qdrant.client.grpc.Collections.ChangeAliases request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateAliasesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Get list of all aliases for a collection
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.ListAliasesResponse listCollectionAliases(io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListCollectionAliasesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Get list of all aliases for all existing collections
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.ListAliasesResponse listAliases(io.qdrant.client.grpc.Collections.ListAliasesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListAliasesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Get cluster information for a collection
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse collectionClusterInfo(io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCollectionClusterInfoMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Check the existence of a collection
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.CollectionExistsResponse collectionExists(io.qdrant.client.grpc.Collections.CollectionExistsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCollectionExistsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Update cluster setup for a collection
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse updateCollectionClusterSetup(io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateCollectionClusterSetupMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Create shard key
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.CreateShardKeyResponse createShardKey(io.qdrant.client.grpc.Collections.CreateShardKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateShardKeyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Delete shard key
     * </pre>
     */
    public io.qdrant.client.grpc.Collections.DeleteShardKeyResponse deleteShardKey(io.qdrant.client.grpc.Collections.DeleteShardKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteShardKeyMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service Collections.
   */
  public static final class CollectionsFutureStub
      extends io.grpc.stub.AbstractFutureStub<CollectionsFutureStub> {
    private CollectionsFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollectionsFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollectionsFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     *Get detailed information about specified existing collection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.GetCollectionInfoResponse> get(
        io.qdrant.client.grpc.Collections.GetCollectionInfoRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Get list name of all existing collections
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.ListCollectionsResponse> list(
        io.qdrant.client.grpc.Collections.ListCollectionsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Create new collection with given parameters
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.CollectionOperationResponse> create(
        io.qdrant.client.grpc.Collections.CreateCollection request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Update parameters of the existing collection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.CollectionOperationResponse> update(
        io.qdrant.client.grpc.Collections.UpdateCollection request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Drop collection and all associated data
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.CollectionOperationResponse> delete(
        io.qdrant.client.grpc.Collections.DeleteCollection request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Update Aliases of the existing collection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.CollectionOperationResponse> updateAliases(
        io.qdrant.client.grpc.Collections.ChangeAliases request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateAliasesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Get list of all aliases for a collection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.ListAliasesResponse> listCollectionAliases(
        io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListCollectionAliasesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Get list of all aliases for all existing collections
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.ListAliasesResponse> listAliases(
        io.qdrant.client.grpc.Collections.ListAliasesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListAliasesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Get cluster information for a collection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse> collectionClusterInfo(
        io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCollectionClusterInfoMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Check the existence of a collection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.CollectionExistsResponse> collectionExists(
        io.qdrant.client.grpc.Collections.CollectionExistsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCollectionExistsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Update cluster setup for a collection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse> updateCollectionClusterSetup(
        io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateCollectionClusterSetupMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Create shard key
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.CreateShardKeyResponse> createShardKey(
        io.qdrant.client.grpc.Collections.CreateShardKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateShardKeyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Delete shard key
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.qdrant.client.grpc.Collections.DeleteShardKeyResponse> deleteShardKey(
        io.qdrant.client.grpc.Collections.DeleteShardKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteShardKeyMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET = 0;
  private static final int METHODID_LIST = 1;
  private static final int METHODID_CREATE = 2;
  private static final int METHODID_UPDATE = 3;
  private static final int METHODID_DELETE = 4;
  private static final int METHODID_UPDATE_ALIASES = 5;
  private static final int METHODID_LIST_COLLECTION_ALIASES = 6;
  private static final int METHODID_LIST_ALIASES = 7;
  private static final int METHODID_COLLECTION_CLUSTER_INFO = 8;
  private static final int METHODID_COLLECTION_EXISTS = 9;
  private static final int METHODID_UPDATE_COLLECTION_CLUSTER_SETUP = 10;
  private static final int METHODID_CREATE_SHARD_KEY = 11;
  private static final int METHODID_DELETE_SHARD_KEY = 12;

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
          serviceImpl.get((io.qdrant.client.grpc.Collections.GetCollectionInfoRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.GetCollectionInfoResponse>) responseObserver);
          break;
        case METHODID_LIST:
          serviceImpl.list((io.qdrant.client.grpc.Collections.ListCollectionsRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.ListCollectionsResponse>) responseObserver);
          break;
        case METHODID_CREATE:
          serviceImpl.create((io.qdrant.client.grpc.Collections.CreateCollection) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse>) responseObserver);
          break;
        case METHODID_UPDATE:
          serviceImpl.update((io.qdrant.client.grpc.Collections.UpdateCollection) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse>) responseObserver);
          break;
        case METHODID_DELETE:
          serviceImpl.delete((io.qdrant.client.grpc.Collections.DeleteCollection) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse>) responseObserver);
          break;
        case METHODID_UPDATE_ALIASES:
          serviceImpl.updateAliases((io.qdrant.client.grpc.Collections.ChangeAliases) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionOperationResponse>) responseObserver);
          break;
        case METHODID_LIST_COLLECTION_ALIASES:
          serviceImpl.listCollectionAliases((io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.ListAliasesResponse>) responseObserver);
          break;
        case METHODID_LIST_ALIASES:
          serviceImpl.listAliases((io.qdrant.client.grpc.Collections.ListAliasesRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.ListAliasesResponse>) responseObserver);
          break;
        case METHODID_COLLECTION_CLUSTER_INFO:
          serviceImpl.collectionClusterInfo((io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse>) responseObserver);
          break;
        case METHODID_COLLECTION_EXISTS:
          serviceImpl.collectionExists((io.qdrant.client.grpc.Collections.CollectionExistsRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CollectionExistsResponse>) responseObserver);
          break;
        case METHODID_UPDATE_COLLECTION_CLUSTER_SETUP:
          serviceImpl.updateCollectionClusterSetup((io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse>) responseObserver);
          break;
        case METHODID_CREATE_SHARD_KEY:
          serviceImpl.createShardKey((io.qdrant.client.grpc.Collections.CreateShardKeyRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.CreateShardKeyResponse>) responseObserver);
          break;
        case METHODID_DELETE_SHARD_KEY:
          serviceImpl.deleteShardKey((io.qdrant.client.grpc.Collections.DeleteShardKeyRequest) request,
              (io.grpc.stub.StreamObserver<io.qdrant.client.grpc.Collections.DeleteShardKeyResponse>) responseObserver);
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
              io.qdrant.client.grpc.Collections.GetCollectionInfoRequest,
              io.qdrant.client.grpc.Collections.GetCollectionInfoResponse>(
                service, METHODID_GET)))
        .addMethod(
          getListMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.ListCollectionsRequest,
              io.qdrant.client.grpc.Collections.ListCollectionsResponse>(
                service, METHODID_LIST)))
        .addMethod(
          getCreateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.CreateCollection,
              io.qdrant.client.grpc.Collections.CollectionOperationResponse>(
                service, METHODID_CREATE)))
        .addMethod(
          getUpdateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.UpdateCollection,
              io.qdrant.client.grpc.Collections.CollectionOperationResponse>(
                service, METHODID_UPDATE)))
        .addMethod(
          getDeleteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.DeleteCollection,
              io.qdrant.client.grpc.Collections.CollectionOperationResponse>(
                service, METHODID_DELETE)))
        .addMethod(
          getUpdateAliasesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.ChangeAliases,
              io.qdrant.client.grpc.Collections.CollectionOperationResponse>(
                service, METHODID_UPDATE_ALIASES)))
        .addMethod(
          getListCollectionAliasesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.ListCollectionAliasesRequest,
              io.qdrant.client.grpc.Collections.ListAliasesResponse>(
                service, METHODID_LIST_COLLECTION_ALIASES)))
        .addMethod(
          getListAliasesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.ListAliasesRequest,
              io.qdrant.client.grpc.Collections.ListAliasesResponse>(
                service, METHODID_LIST_ALIASES)))
        .addMethod(
          getCollectionClusterInfoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.CollectionClusterInfoRequest,
              io.qdrant.client.grpc.Collections.CollectionClusterInfoResponse>(
                service, METHODID_COLLECTION_CLUSTER_INFO)))
        .addMethod(
          getCollectionExistsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.CollectionExistsRequest,
              io.qdrant.client.grpc.Collections.CollectionExistsResponse>(
                service, METHODID_COLLECTION_EXISTS)))
        .addMethod(
          getUpdateCollectionClusterSetupMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupRequest,
              io.qdrant.client.grpc.Collections.UpdateCollectionClusterSetupResponse>(
                service, METHODID_UPDATE_COLLECTION_CLUSTER_SETUP)))
        .addMethod(
          getCreateShardKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.CreateShardKeyRequest,
              io.qdrant.client.grpc.Collections.CreateShardKeyResponse>(
                service, METHODID_CREATE_SHARD_KEY)))
        .addMethod(
          getDeleteShardKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.qdrant.client.grpc.Collections.DeleteShardKeyRequest,
              io.qdrant.client.grpc.Collections.DeleteShardKeyResponse>(
                service, METHODID_DELETE_SHARD_KEY)))
        .build();
  }

  private static abstract class CollectionsBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CollectionsBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.qdrant.client.grpc.CollectionsService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Collections");
    }
  }

  private static final class CollectionsFileDescriptorSupplier
      extends CollectionsBaseDescriptorSupplier {
    CollectionsFileDescriptorSupplier() {}
  }

  private static final class CollectionsMethodDescriptorSupplier
      extends CollectionsBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    CollectionsMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (CollectionsGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CollectionsFileDescriptorSupplier())
              .addMethod(getGetMethod())
              .addMethod(getListMethod())
              .addMethod(getCreateMethod())
              .addMethod(getUpdateMethod())
              .addMethod(getDeleteMethod())
              .addMethod(getUpdateAliasesMethod())
              .addMethod(getListCollectionAliasesMethod())
              .addMethod(getListAliasesMethod())
              .addMethod(getCollectionClusterInfoMethod())
              .addMethod(getCollectionExistsMethod())
              .addMethod(getUpdateCollectionClusterSetupMethod())
              .addMethod(getCreateShardKeyMethod())
              .addMethod(getDeleteShardKeyMethod())
              .build();
        }
      }
    }
    return result;
  }
}
