package cn.com.za.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import cn.com.za.annotation.Route;
import cn.com.za.annotation.model.RouteMeta;
import cn.com.za.compiler.utils.Consts;
import cn.com.za.compiler.utils.Log;
import cn.com.za.compiler.utils.Utils;

@AutoService(Processor.class)
// 处理器接收的参数
@SupportedOptions(Consts.ARGUMENTS_NAME)
//注册给哪些注解
@SupportedAnnotationTypes({Consts.ANN_TYPE_ROUTE})
//指定使用java版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class RouterProcessor extends AbstractProcessor {

    /**
     * 节点工具类，（类，函数，属性都是节点）
     */
    private Elements mElementUtils;
    /**
     * 文件生成器 类/资源
     */
    private Filer mFilerUtils;
    /**
     * 类信息工具
     */
    private Types mTypeUtils;
    private String mModelName;

    Log log;

    /**
     * 分组对应的路由信息，key为组名，value为路由的名字
     */
    private Map<String, List<RouteMeta>> mGroupMap = new HashMap<>();

    /**
     * key:组名 value:类名
     */
    private Map<String, String> mRootMap = new TreeMap<>();


    /**
     * 初始化 初始化一些工具类
     *
     * @param processingEnv
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        log = Log.newLog(processingEnv.getMessager());

        mElementUtils = processingEnv.getElementUtils();
        mFilerUtils = processingEnv.getFiler();
        mTypeUtils = processingEnv.getTypeUtils();
        Map<String, String> options = processingEnv.getOptions();
        if (!Utils.isEmpty(options)) {
            mModelName = options.get(Consts.ARGUMENTS_NAME);
        }
        log.i("RouteProcessor Parmaters:" + mModelName);
        if (Utils.isEmpty(mModelName)) {
            throw new RuntimeException("===Not Set RouteProcessor Parmaters");
        }
    }

    /**
     * 相当于main函数，处理注解
     *
     * @param annotations 注解的节点集合
     * @param roundEnv    运行环境，可以通过该对象查找到注解
     * @return true 表示后续处理器不会再处理（已经处理）
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        log.i("process start");
        if (!Utils.isEmpty(annotations)) {
            //被Route注解，注解的节点集合
            Set<? extends Element> routeElements
                    = roundEnv.getElementsAnnotatedWith(Route.class);
            if (!Utils.isEmpty(routeElements)) {
                processRoute(routeElements);
            }
            return true;
        }
        return false;
    }

    /**
     * 处理注解
     *
     * @param routeElements
     */
    private void processRoute(Set<? extends Element> routeElements) {
        //获取Activity的节点信息
        TypeElement activityElement = mElementUtils.getTypeElement(Consts.ACTIVITY);
        RouteMeta routeMeta;

        TypeElement serviceElement = mElementUtils.getTypeElement(Consts.ISERVICE);

        for (Element element : routeElements) {
            //类信息
            TypeMirror typeMirror = element.asType();
            log.i("Class Name:" + typeMirror.toString());
            Route route = element.getAnnotation(Route.class);

            if (mTypeUtils.isSubtype(typeMirror, activityElement.asType())) {
                //只能在Activity上使用
                routeMeta = new RouteMeta(RouteMeta.Type.ACTIVITY, route, element);
            } else if (mTypeUtils.isSubtype(typeMirror,serviceElement.asType())){
                routeMeta = new RouteMeta(RouteMeta.Type.ISERVICE, route, element);
            }else {
                throw new RuntimeException("[Just support] Activity Route：" + element);
            }
            //检查是否配置了group，没有配置就从path中截取
            checkHasGroupOrNot(routeMeta);

        }
        TypeElement iRouteGroup = mElementUtils.getTypeElement(Consts.IROUTE_GROUP);
        //生产路由表
        TypeElement iRouteRoot = mElementUtils.getTypeElement(Consts.IROUTE_ROOT);
        //生成分组表 $$Group$$
        generatedGroup(iRouteGroup);

        generatedRoot(iRouteRoot, iRouteGroup);
    }

    /**
     * 生成路由表
     *
     * @param iRouteRoot
     * @param iRouteGroup
     */
    private void generatedRoot(TypeElement iRouteRoot, TypeElement iRouteGroup) {
        //类型 Map<String,Class<? extends IRouteGroup>> routes>
        ParameterizedTypeName parameterizedType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(iRouteGroup))
                ));
        //参数 Map<String,Class<? extends IRouteGroup>> routes> routes
        ParameterSpec rootParamSpec = ParameterSpec.builder(parameterizedType, "routes")
                .build();
        //创建函数 public void loadInfo(Map<String,Class<? extends IRouteGroup>> routes> routes)
        MethodSpec.Builder builder = MethodSpec.methodBuilder(Consts.METHOD_LOAD_INTO)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(rootParamSpec);

        //添加函数体
        for (Map.Entry<String, String> entry : mRootMap.entrySet()) {
            builder.addStatement("routes.put($S,$T.class)", entry.getKey()
                    , ClassName.get(Consts.PACKAGE_OF_GENERATE_FILE, entry.getValue()));
        }

        //生成类 $Root$
        String rootClassName = Consts.NAME_OF_ROOT + mModelName;
        TypeSpec typeSpec = TypeSpec.classBuilder(rootClassName)
                .addSuperinterface(ClassName.get(iRouteRoot))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(builder.build())
                .build();

        JavaFile javaFile = JavaFile.builder(Consts.PACKAGE_OF_GENERATE_FILE, typeSpec).build();
        try {
            javaFile.writeTo(mFilerUtils);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param iRouteGroup
     */
    private void generatedGroup(TypeElement iRouteGroup) {



        // 创建参数类型  Map<String, RouteMeta>
        ParameterizedTypeName parameterizedType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouteMeta.class)
        );
        log.i("generatedGroup");
        //创建参数 Map<String, RouteMeta> atlas
        ParameterSpec atlas = ParameterSpec.builder(parameterizedType, "atlas").build();
        //遍历每一个分组 创建Group类
        for (Map.Entry<String, List<RouteMeta>> entry : mGroupMap.entrySet()) {
            //创建方法名 @Override public void loadInto(Map<String, RouteMeta> atlas)
            MethodSpec.Builder method = MethodSpec.methodBuilder("loadInto")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(atlas);
            //获取组名
            String groupName = entry.getKey();
            List<RouteMeta> groupData = entry.getValue();
            for (RouteMeta routeMeta : groupData) {
                //添加函数体
                //atlas.put("/main/test", RouteMeta.build(RouteMeta.Type.ACTIVITY,SecondActivity.class, "/main/test", "main"));
                // $S = String
                // $T = Class 类
                // $L = 字面量
                //
                method.addStatement("atlas.put($S,$T.build($T.$L,$T.class,$S,$S))",
                        routeMeta.getPath(),
                        ClassName.get(RouteMeta.class),
                        ClassName.get(RouteMeta.Type.class),
                        routeMeta.getType(),
                        ClassName.get((TypeElement) routeMeta.getElement()),
                        routeMeta.getPath(),
                        routeMeta.getGroup());
            }
            //类名
            String groupClassName = Consts.NAME_OF_GROUP + groupName;
            //创建类
            TypeSpec typeSpec = TypeSpec.classBuilder(groupClassName)
                    .addSuperinterface(ClassName.get(iRouteGroup))
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(method.build()).build();
            //创建java类
            JavaFile javaFile = JavaFile.builder(Consts.PACKAGE_OF_GENERATE_FILE, typeSpec).build();
            try {
                javaFile.writeTo(mFilerUtils);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mRootMap.put(groupName, groupClassName);
        }
    }

    /**
     * 检查是否设置了组名
     *
     * @param routeMeta
     */
    private void checkHasGroupOrNot(RouteMeta routeMeta) {
        if (routeVerify(routeMeta)) {
            //缓存分组与分组中的路由信息
            log.i("Group :" + routeMeta.getGroup() + " path=" + routeMeta.getPath());
            List<RouteMeta> routeMetas = mGroupMap.get(routeMeta.getGroup());
            if (Utils.isEmpty(routeMetas)) {
                routeMetas = new ArrayList<>();
                routeMetas.add(routeMeta);
                mGroupMap.put(routeMeta.getGroup(), routeMetas);
            } else {
                routeMetas.add(routeMeta);
            }

        } else {
            log.i("Group Info Error:" + routeMeta.getPath());
        }
    }


    private boolean routeVerify(RouteMeta routeMeta) {

        String path = routeMeta.getPath();
        String group = routeMeta.getGroup();

        if (!path.startsWith("/")) {
            return false;
        }
        //如果没有设置分组group则从path中截取
        if (Utils.isEmpty(group)) {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            if (Utils.isEmpty(defaultGroup)) {
                return false;
            }
            //设置默认的分组
            routeMeta.setGroup(defaultGroup);
        }

        return true;
    }
}
