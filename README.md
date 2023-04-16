# description
Android HiddenApiByPasser for Android 9 - Android 13

# example
```java
// ByPass All HiddenApi
Reflection.bypassAll();

// ByPass Target Hidden Methods
String[] methods = new String[]{"name1", "name2"}
Reflection.bypass(methods)

// set object field
Reflection.set(obj, "name", value);
// set static field
Reflection.setStatic(class, "name", value);

// get object field
Reflection.get(obj, "name", value);
// get static field
Reflection.getStatic(class, "name", value);

// invoke object method
Reflection.invoke(obj, "name", args);
// invoke static method
Reflection.invokeStatic(class, "name", args);
```

# refer
> https://lovesykun.cn/archives/android-hidden-api-bypass.html
