jpa代码环境：
lesson-1-sms-interface项目中创建 jpa包 com.dongnaoedu.springcloud.jpa
什么时JPA？
JPA顾名思义就是Java Persistence API的意思，是JDK 5.0注解或XML描述对象－关系表的映射关系，并将运行期的实体对象持久化到数据库中。
JPA优势？
2.1标准化
JPA 是 JCP 组织发布的 Java EE 标准之一，因此任何声称符合 JPA 标准的框架都遵循同样的架构，提供相同的访问API，这保证了基于JPA开发的企业应用能够经过少量的修改就
能够在不同的JPA框架下运行。
2.2容器级特性的支持
JPA框架中支持大数据集、事务、并发等容器级事务，这使得 JPA 超越了简单持久化框架的局限，在企业应用发挥更大的作用。
2.3简单方便
JPA的主要目标之一就是提供更加简单的编程模型：在JPA框架下创建实体和创建Java 类一样简单，没有任何的约束和限制，只需要使用 javax.persistence.Entity进行注释，
JPA的框架和接口也都非常简单，没有太多特别的规则和设计模式的要求，开发者可以很容易的掌握。JPA基于非侵入式原则设计，因此可以很容易的和其它框架或者容器集成。
2.4查询能力
JPA的查询语言是面向对象而非面向数据库的，它以面向对象的自然语法构造查询语句，可以看成是Hibernate HQL的等价物。JPA定义了独特的JPQL（Java Persistence Query
Language），JPQL是EJB QL的一种扩展，它是针对实体的一种查询语言，操作对象是实体，而不是关系数据库的表，而且能够支持批量更新和修改、JOIN、GROUP BY、HAVING 等
通常只有 SQL 才能够提供的高级查询特性，甚至还能够支持子查询。
2.4高级特性
JPA 中能够支持面向对象的高级特性，如类之间的继承、多态和类之间的复杂关系，这样的支持能够让开发者最大限度的使用面向对象的模型设计企业应用，而不需要自行处理这些
特性在关系数据库的持久化。
-----------------------------------------------------------------------------------------------------
bootstrap.yml文件中配置 mysql数据源及 jpa，如下：
spring:
  application:
    name: lesson-1-sms-interface
  datasource:
      driver-class-name: com.mysql.jdbc.Driver
      url: jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&characterEncoding=utf8&characterSetResults=utf8
      username: root
      password: 123456
  jpa:
      hibernate:
        ddl-auto: update  # 不要用create，会删除表；都使用update，第一次会创建表，以后更新表
        show-sql: true
        format-sql: true
  data:
      rest:
        base-path: /api  # 此时REST资源的路径变成了http://localhost:9002/api/workers
  thymeleaf:
    cache: false    # 关闭页面缓存，修改文件后，直接刷新，不需重启
------------------------
pom.xml文件 jpa依赖包如下：
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.hsqldb</groupId>
    <artifactId>hsqldb</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
------------------------
..jpa.domain包中创建 Person类，如下：
@Entity
@Table(name = "t_person")
public class Person {
	@Id
	@GeneratedValue
	private Long id;
	private String name;
	private Integer age;
	private String address;
	setter，getter省略
}
------------------------
..jpa.dao包中创建 PersonRepository类，如下：
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

}
------------------------
..jpa.controller包中创建 PersonController类，如下：
@RestController
public class PersonController {
    @Autowired
    private PersonRepository personRepository;
}
------------------------
PersonController类中创建 save()方法，save()为JpaRepository借口中方法，如下：
public class PersonController {
    @Autowired
    private PersonRepository personRepository;

    @RequestMapping(value = "/save",method = RequestMethod.GET)
    public Person save(String name, String address, Integer age){
        Person person = personRepository.save(new Person(null,name,age,address));
        return person;
    }
}
发送get请求：http://localhost:9002/save?name=Kobe&age=36&address=USA，
页面返回：{"id":3,"name":"Kobe","age":36,"address":"USA"}，数据库中已成功添加该数据；
**************************************************************************************************************************************************
重点：当Person对象中设置主键时 person.setId(id)，则是把主键对应的数据 进行更新；
**************************************************************************************************************************************************
jpa删除数据，只需传入主键即可：personRepository.delete(id);
------------------------
PersonController类中创建 findByName()方法，该方法为 使用方法名查询，不是JpaRepository接口方法，因此需要
在PersonRepository接口中定义该方法，扩展父接口JpaRepository，具体如下：
public class PersonController {
    @Autowired
    private PersonRepository personRepository;

    @RequestMapping(value = "/findByNameAndAddress",method = RequestMethod.GET)
    public List<Person> findByName(String name,String address){
        List<Person> persons = personRepository.findByNameAndAddress(name,address);
        return persons;
    }
}

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    /**使用方法名查询，接收参数，返回一个列表*/
    List<Person> findByNameAndAddress(String name,String address);
}
发送get请求：http://localhost:9002/findByNameAndAddress?name=Kobe&address=USA，
页面返回：{"id":3,"name":"Kobe","age":36,"address":"USA"}
------------------------
通过继承JpaRepository接口，除了可以获得基础CRUD操作方法之外，
接口方法中通过定义@Query annotation自定义接口方法的JPQL语句
还可以通过Spring规定的接口命名方法自动创建
复杂的CRUD操作，以下是我在Spring Data JPA 文档中找到的命名规则表：
Keyword 	    Sample	                                JPQL snippet
And	            findByLastnameAndFirstname	            … where x.lastname = ?1 and x.firstname = ?2
Or	            findByLastnameOrFirstname	            … where x.lastname = ?1 or x.firstname = ?2
Is,Equals	    findByFirstname,findByFirstnameIs,findByFirstnameEquals	… where x.firstname = ?1
Between	        findByStartDateBetween	                … where x.startDate between ?1 and ?2
LessThan	    findByAgeLessThan	                    … where x.age < ?1
LessThanEqual   findByAgeLessThanEqual	            … where x.age <= ?1
GreaterThan	    findByAgeGreaterThan	                … where x.age > ?1
GreaterThanEqual findByAgeGreaterThanEqual	        … where x.age >= ?1
After	        findByStartDateAfter	                … where x.startDate > ?1
Before	        findByStartDateBefore	                … where x.startDate < ?1
IsNull	        findByAgeIsNull	… where x.age is null
IsNotNull,NotNull findByAge(Is)NotNull	            … where x.age not null
Like	        findByFirstnameLike	                    … where x.firstname like ?1
NotLike	        findByFirstnameNotLike	… where x.firstname not like ?1
StartingWith    findByFirstnameStartingWith	        … where x.firstname like ?1 (parameter bound with appended %)
EndingWith	    findByFirstnameEndingWith	            … where x.firstname like ?1 (parameter bound with prepended %)
Containing	    findByFirstnameContaining	            … where x.firstname like ?1 (parameter bound wrapped in %)
OrderBy	        findByAgeOrderByLastnameDesc	        … where x.age = ?1 order by x.lastname desc
Not	            findByLastnameNot	… where x.lastname <> ?1
In	            findByAgeIn(Collection<Age> ages)	    … where x.age in ?1
NotIn	        findByAgeNotIn(Collection<Age> age) 	… where x.age not in ?1
TRUE	        findByActiveTrue()	                    … where x.active = true
FALSE	        findByActiveFalse()	                    … where x.active = false
IgnoreCase	    findByFirstnameIgnoreCase	            … where UPPER(x.firstame) = UPPER(?1)
-------------------------------------------------------------------------------------------------------------------------------------------------


使用@Query注解，自定义sql：
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    @Query(nativeQuery = true,value = "select * from t_person where age > ?1")  # ?1代表 ?接收nativeQuery(int age)方法中第一个参数；
    public List<Object> nativeQuery(int age);
}

使用@Query配合@Modifying，自定义sql；@Query + @Modifying + @Transactional = 增 删 改，其中@Transactional表示自动事务管理；
@Modifying
@Transactional(readOnly = false)
@Query("update Person p set p.age=:age where name=:name")   /**JPSL可移植性，换什么数据库都可以*/
int updateAgeByName(@Param("name") String name,@Param("age") int age);

自定义BaseRepository：
正常情况下不仅仅只继承一个JpaRepository接口，下一章整合SpringDataJPA跟QueryDSL时就需要添加多个接口继承了，因此创建base包，包内创建BaseRepository并且继承
JpaRepository
@NoRepositoryBean   /**@NoRepositoryBean注释的 接口，不会被作为Repository，不会为其创建代理类*/
public interface BaseRepository<T,PK extends Serializable> extends JpaRepository<T,PK> {
}

分页查询：
一般会创建一个BaseEntity，在BaseEntity内添加几个字段：排序列，排序方式，当前页码，每页条数等，代码如下，其中引入 lombok包和lombok-maven-plugin插件，通过注解
@Setter @Getter生成 Getter()、Setter()方法；