V tomto priečinku sa nachádza zdrojový kód pluginu Community Detection pre platformu Gephi 0.9.5.

## Návod na prácu s pluginom
### Inštalácia do repozitára gephi-plugins

* Súbor CommunityDetection je potrebné vložiť do priečinka /modules v repozitári *gephi-plugins* (https://github.com/gephi/gephi-plugins/)

* V gephi-plugins/pom.xml je potrebné pridať adresu modulu do zoznamu pod riadok:
```
        <!-- Add here the paths of all modules (e.g. <module>modules/MyModule</module>) -->
```
ako:
```
        <module>modules/CommunityDetection</module>
```
* Nahradiť NBM plugin: 
```
        <!-- NBM Plugin -->
        <plugin>
            <groupId>org.apache.netbeans.utilities</groupId>
            <artifactId>nbm-maven-plugin</artifactId>
            <configuration>
                <nbmBuildDir>${clusters.path}</nbmBuildDir>
            </configuration>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>cluster</goal>
                    </goals>
                </execution>
                <!-- Disable default-branding as it's only needed for the branding module -->
                <execution>
                    <id>default-branding</id>
                    <phase>none</phase>
                </execution>
            </executions>
        </plugin>
```
pluginom:
```
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>nbm-maven-plugin</artifactId>
            <version>3.14</version>
        </plugin>
```
### Generovanie pluginu a spustenie Gephi
Po úspešnej konfigurácii v repozitári *gephi-plugins* by mali správne fungovať Maven goals.

* Na vytvorenie zbaleného pluginu sa môže použiť goal *package*.

* Na spustenie Gephi slúži goal *gephi:run*.

Po spustení Gephi je potrebné plugin manuálne inštalovať v ponuke *Tools>Plugins>Downloaded*.

Zbalený plugin je vygenerovaný v */modules/CommunityDetection/target* .
