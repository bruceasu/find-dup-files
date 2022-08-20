package me.asu.fdf;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.ToString;

/**
 * 处理的目录
 */
@Getter
@ToString
public class HandlerPath {

    static class InstanceHolder {
       static HandlerPath instance =  new HandlerPath();
    }

    /**
     * 包含的目录
     */
    private Set<String> includePath = new HashSet<>();

    /**
     *排除的目录
     */
    private Set<String> excludepath = new HashSet<>();

    private HandlerPath(){}

    public void addIncludePath(String path){
        this.includePath.add(path);
    }
    public void addIncludePaths(String... path){
        this.includePath.addAll(Arrays.asList(path));
    }
    public void addExcludePath(String path){
        this.excludepath.add(path);
    }

    public void addExcludePaths(String... path){
        this.excludepath.addAll(Arrays.asList(path));
    }

    public static HandlerPath getInstance(){
        return InstanceHolder.instance;
    }

}
