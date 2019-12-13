Nifi Headless Browser Processor
================================

**Currently, `URL Provided` configuration is only tested.**

* Returns the page source in its current state to FlowFile, including any DOM updates that occurred after page load.
* Use [JBrowserDriver](https://github.com/MachinePublishers/jBrowserDriver).

# Prerequisite

* JRE with Java FX
  * OpenJDK 8 does not contain `Java FX`
  * Use Oracle JDK or Zulu JDK FX
  
* `fontconfig` package on OS.
  * `yum install fontconfig` or `apt install fontconfig`

# Configurations

Most configuration is used to make JBrowserDriver.

* configurations
  * Host: Hostname for the browser. hostname or ip address.
  * Url Provided: if true, the processor read target from `Page URL` configuration. if false, the input flowfile must contain URL.
  * Page URL: URL for processing. Used only `Url Provided` is `true`.
  * Timezone: Timezone for browser. Select from dropdown list.
  * Port Range: port range for JBrowserDriverServer. This range should be multiple of three.
  * ~~Javascript: Script after page loading. Currently, EL is not supported.~~
    * Remove for now due to timing issue.

* Relationship
  * success: success relationship of this processor. Flowfile contains page source of input URL.
  * failure: failure relationship of this processor.
 
# TODOs

- [ ] Test for `Url Provided: false` configuration.
- [ ] Add some attribute to result flowfile.
  - [x] Source URL
  - [ ] Page Title
  - [ ] Etc.
- [ ] Add capabilities to execute javascript after page loading. 