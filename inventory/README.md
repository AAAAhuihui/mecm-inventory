# mecm-inventory

#### 描述
MECM-Inventory 模块提供 MEC 系统的所有系统和主机级资源的公共清单。

#### 编译和构建
Inventory项目基于docker容器化，在编译和构建过程中分为两个步骤。

#### 编译
Inventory是一个基于jdk1.8和maven编写的Java程序。 编译只需执行 mvn install 即可编译生成jar包

#### 编译父依赖仓库
  - 配置环境，安装jdk1.8
    ```
      sudo apt install openjdk-8-jdk
      export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
      export PATH=$JAVA_HOME/bin:$PATH
      mvn -v
      mvn clean install
    ```
  - 配置mvn源
    ```
      mkdir -p ~/.m2

      cat > ~/.m2/settings.xml <<EOF
      <settings>
        <mirrors>
          <mirror>
            <id>aliyun</id>
            <mirrorOf>*</mirrorOf>
            <url>https://maven.aliyun.com/repository/public</url>
          </mirror>
        </mirrors>
      </settings>
      EOF

      rm -rf ~/.m2/repository/org/apache/maven
      mvn clean install -U
      ```
  - 拉取代码
    ```
     git clone https://gitee.com/edgegallery/eg-parent.git
    ```
  - 安装依赖
    ```
     mvn clean install
    ```
#### 编译inventory

   - 拉取代码
     ```
      git clone https://gitee.com/edgegallery/mecm-inventory.git
     ```
   - 安装依赖
     ```
      # maven clean install

      if [ -x ./mvnw ]; then
        ./mvnw -DskipTests clean package
      else
        mvn -DskipTests clean package
      fi
     ```

#### 构建镜像
Inventory 项目提供了一个用于镜像的 dockerfile 文件。 制作镜像时可以使用以下命令

```shell
cd inventory
docker build -t edgegallery/mecm-inventory:latest -f docker/Dockerfile .
```
