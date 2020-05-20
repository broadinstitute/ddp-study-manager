//package org.broadinstitute.dsm.route;
//
//import com.google.gson.Gson;
//import org.broadinstitute.ddp.handlers.util.Result;
//import org.broadinstitute.dsm.db.KitRequestShipping;
//import org.broadinstitute.dsm.db.KitRequestCreateLabel;
//import org.broadinstitute.dsm.security.RequestHandler;
//import org.broadinstitute.dsm.statics.RoutePath;
//import org.broadinstitute.dsm.statics.UserErrorMessages;
//import org.broadinstitute.dsm.util.DBUtil;
//import org.broadinstitute.dsm.util.KitUtil;
//import org.broadinstitute.dsm.util.UserUtil;
//import spark.QueryParamsMap;
//import spark.Request;
//import spark.Response;
//
//public class KitLabelRoute extends RequestHandler {
//
//    @Override
//    public Object processRequest(Request request, Response response, String userId, String userMail) throws Exception {
//        if (RoutePath.RequestMethod.GET.toString().equals(request.requestMethod())) {
//            return new Result(200, String.valueOf(DBUtil.getBookmark(KitUtil.BOOKMARK_LABEL_CREATION_RUNNING)));
//        }
//        else {
//            if (UserUtil.checkUserAccess(null, userId, "kit_shipping")) {
//                String userIdRequest = UserUtil.getUserId(request);
//                if (!userId.equals(userIdRequest)) {
//                    throw new RuntimeException("User id was not equal. User Id in token " + userId + " user Id in request " + userIdRequest);
//                }
//                QueryParamsMap queryParams = request.queryMap();
//
//                String requestBody = request.body();
//                if (requestBody != null) {
//                    KitRequestShipping[] kitRequests = new Gson().fromJson(requestBody, KitRequestShipping[].class);
//                    if (kitRequests != null) {
//                        KitRequestCreateLabel.updateKitLabelRequested(kitRequests, userIdRequest);
//                        return new Result(200);
//                    }
//                }
//
//                String realm = null;
//                if (queryParams.value(RoutePath.REALM) != null) {
//                    realm = queryParams.get(RoutePath.REALM).value();
//                }
//                String kitType = null;
//                if (queryParams.value(RoutePath.KIT_TYPE) != null) {
//                    kitType = queryParams.get(RoutePath.KIT_TYPE).value();
//                }
//                KitRequestCreateLabel.updateKitLabelRequested(realm, kitType, userIdRequest);
//                return new Result(200);
//            }
//            else {
//                response.status(500);
//                return new Result(500, UserErrorMessages.NO_RIGHTS);
//            }
//        }
//    }
//}
