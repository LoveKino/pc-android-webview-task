/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};

/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {

/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId])
/******/ 			return installedModules[moduleId].exports;

/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			exports: {},
/******/ 			id: moduleId,
/******/ 			loaded: false
/******/ 		};

/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);

/******/ 		// Flag the module as loaded
/******/ 		module.loaded = true;

/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}


/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;

/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;

/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";

/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(0);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	let {
	    pc
	} = __webpack_require__(1);

	let send = (data) => {
	    window.__webView_bridge.accept(JSON.stringify(data));
	};

	let call = pc((handle) => {
	    window.__onWebViewMessage = (data) => {
	        handle(data, send);
	    };
	}, send, {
	    subtraction: (a, b) => {
	        return a - b;
	    }
	});

	call('add', [1, 2]).then((ret) => {
	    console.log(ret);
	});


/***/ },
/* 1 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__(2);


/***/ },
/* 2 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	let {pc} = __webpack_require__(3);

	let {stringify, parseJSON} = __webpack_require__(15);

	module.exports = {
	    stringify,
	    parseJSON,
	    pc
	};


/***/ },
/* 3 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	// TODO support high order function

	let {
	    likeArray, funType, isFalsy, or, isFunction, isString, isObject
	} = __webpack_require__(4);

	let messageQueue = __webpack_require__(5);

	let callFunction = __webpack_require__(9);

	let wrapListen = __webpack_require__(14);

	let idgener = __webpack_require__(7);

	let {
	    map, forEach
	} = __webpack_require__(11);

	let pc = funType((listen, send, sandbox) => {
	    // data = {id, error, data}
	    let {
	        consume, produce
	    } = messageQueue();

	    let box = getBox(sandbox);

	    // reqData = {id, source, time}
	    let reqHandler = ({
	        id, source
	    }, send) => {
	        let sendRes = sender('response', send);

	        let ret = dealReq(source, box, call);

	        return packRes(ret, id).then(sendRes).then(() => ret);
	    };

	    listen = wrapListen(listen, send);

	    let listenHandle = listenHandler(reqHandler, (ret) => {
	        if (ret.error) {
	            let err = new Error(ret.error.msg);
	            err.stack = ret.error.stack;
	            ret.error = err;
	        }
	        return consume(ret);
	    });

	    // data = {id, source, time}
	    let sendReq = sender('request', send);

	    let watch = listen(listenHandle);

	    let catchSendReq = (data) => {
	        try {
	            watch(data, sendReq(data));
	        } catch (err) {
	            consume({
	                id: data.id,
	                error: err
	            });
	        }
	    };

	    let call = funType((name, args = [], type = 'public') => {
	        // data = {id, source, time}
	        let {
	            data, result
	        } = produce(packReq(name, args, type, box));

	        catchSendReq(data);

	        let clearCallback = () => forEach(args, (arg) => {
	            if (isFunction(arg) && arg.onlyInCall) {
	                box.systembox.removeCallback(arg);
	            }
	        });

	        result.then(clearCallback).catch(clearCallback);

	        return result;
	    }, [isString, or(likeArray, isFalsy), or(isString, isFalsy)]);

	    // detect connection
	    detect(call);

	    return call;
	}, [or(isFalsy, isFunction), or(isFalsy, isFunction), or(isFalsy, isObject)]);

	let listenHandler = (reqHandle, resHandle) => ({
	    type, data
	}, send) => {
	    if (type === 'response') {
	        return resHandle(data, send);
	    } else if (type === 'request') {
	        return reqHandle(data, send);
	    }
	};

	let sender = (type, send) => (data) => {
	    return send({
	        type, data
	    });
	};

	let getBox = (sandbox) => {
	    let callbackMap = {};

	    return {
	        systembox: {
	            detect: () => true,

	            addCallback: funType((callback) => {
	                let id = idgener();
	                callback.callId = id;
	                callbackMap[id] = callback;
	                return id;
	            }, [isFunction]),

	            callback: (id, args) => {
	                let fun = callbackMap[id];
	                if (!fun) {
	                    throw new Error(`missing callback function for id ${id}`);
	                }

	                return fun.apply(undefined, args);
	            },

	            removeCallback: (callback) => {
	                delete callbackMap[callback.callId];
	            }
	        },

	        sandbox
	    };
	};

	let detect = (call) => {
	    let tryCall = () => {
	        return Promise.race([
	            new Promise((resolve, reject) => {
	                setTimeout(reject, 1000);
	            }), call('detect', null, 'system')
	        ]);
	    };
	    // detect connection
	    call.detect = (tryTimes = 10) => {
	        if (tryTimes < 0) return Promise.resolve(false);

	        return tryCall().catch(() => {
	            return call.detect(--tryTimes);
	        });
	    };
	};

	let packReq = (name, args, type, box) => {
	    return {
	        type,
	        name,
	        args: map(args || [], (arg) => isFunction(arg) ? {
	            type: 'function',
	            arg: box.systembox.addCallback(arg)
	        } : {
	            type: 'jsonItem',
	            arg
	        })
	    };
	};

	let unPackReq = (source, call) => {
	    // process args
	    source.args = map(source.args, ({
	        type, arg
	    }) => type === 'function' ? (...fargs) => call('callback', [arg, fargs], 'system') : arg);

	    return source;
	};

	let dealReq = (source, box, call) => {
	    let {
	        type, name, args
	    } = unPackReq(source, call);
	    let sbox = getSBox(box, type);
	    if (sbox) {
	        return callFunction(sbox, name, args);
	    } else {
	        return new Error(`missing sandbox for ${type}`);
	    }
	};

	let getSBox = ({
	    sandbox, systembox
	}, type) => {
	    if (type === 'public') {
	        return sandbox;
	    } else if (type === 'system') {
	        return systembox;
	    }
	    return false;
	};

	let packRes = (ret, id) => {
	    return Promise.resolve(ret).then((ret) =>
	        (ret instanceof Error) ? getErrorRes(ret, id) : {
	            data: ret,
	            id
	        }
	    ).catch(err => getErrorRes(err, id));
	};

	let getErrorRes = (err, id) => {
	    return {
	        error: {
	            msg: getErrorMsg(err),
	            stack: err.stack
	        },
	        id
	    };
	};

	let getErrorMsg = (err) => {
	    let str = err.toString();
	    let type = str.split(':')[0];
	    return str.substring(type.length + 1).trim();
	};

	module.exports = {
	    pc
	};


/***/ },
/* 4 */
/***/ function(module, exports) {

	'use strict';

	/**
	 * basic types
	 */

	let isUndefined = v => v === undefined;

	let isNull = v => v === null;

	let isFalsy = v => !v;

	let likeArray = v => !!(v && typeof v === 'object' && typeof v.length === 'number' && v.length >= 0);

	let isArray = v => Array.isArray(v);

	let isString = v => typeof v === 'string';

	let isObject = v => !!(v && typeof v === 'object');

	let isFunction = v => typeof v === 'function';

	let isNumber = v => typeof v === 'number' && !isNaN(v);

	let isBool = v => typeof v === 'boolean';

	let isNode = (o) => {
	    return (
	        typeof Node === 'object' ? o instanceof Node :
	        o && typeof o === 'object' && typeof o.nodeType === 'number' && typeof o.nodeName === 'string'
	    );
	};

	let isPromise = v => v && typeof v === 'object' && typeof v.then === 'function' && typeof v.catch === 'function';

	let isRegExp = v => v instanceof RegExp;

	let isReadableStream = (v) => isObject(v) && isFunction(v.on) && isFunction(v.pipe);

	let isWritableStream = v => isObject(v) && isFunction(v.on) && isFunction(v.write);

	/**
	 * check type
	 *
	 * types = [typeFun]
	 */
	let funType = (fun, types = []) => {
	    if (!isFunction(fun)) {
	        throw new TypeError(typeErrorText(fun, 'function'));
	    }

	    if (!likeArray(types)) {
	        throw new TypeError(typeErrorText(types, 'array'));
	    }

	    for (let i = 0; i < types.length; i++) {
	        let typeFun = types[i];
	        if (typeFun) {
	            if (!isFunction(typeFun)) {
	                throw new TypeError(typeErrorText(typeFun, 'function'));
	            }
	        }
	    }

	    return function() {
	        // check type
	        for (let i = 0; i < types.length; i++) {
	            let typeFun = types[i];
	            let arg = arguments[i];
	            if (typeFun && !typeFun(arg)) {
	                throw new TypeError(`Argument type error. Arguments order ${i}. Argument is ${arg}.`);
	            }
	        }
	        // result
	        return fun.apply(this, arguments);
	    };
	};

	let and = (...args) => {
	    if (!any(args, isFunction)) {
	        throw new TypeError('The argument of and must be function.');
	    }
	    return (v) => {
	        for (let i = 0; i < args.length; i++) {
	            let typeFun = args[i];
	            if (!typeFun(v)) {
	                return false;
	            }
	        }
	        return true;
	    };
	};

	let or = (...args) => {
	    if (!any(args, isFunction)) {
	        throw new TypeError('The argument of and must be function.');
	    }

	    return (v) => {
	        for (let i = 0; i < args.length; i++) {
	            let typeFun = args[i];
	            if (typeFun(v)) {
	                return true;
	            }
	        }
	        return false;
	    };
	};

	let not = (type) => {
	    if (!isFunction(type)) {
	        throw new TypeError('The argument of and must be function.');
	    }
	    return (v) => !type(v);
	};

	let any = (list, type) => {
	    if (!likeArray(list)) {
	        throw new TypeError(typeErrorText(list, 'list'));
	    }
	    if (!isFunction(type)) {
	        throw new TypeError(typeErrorText(type, 'function'));
	    }

	    for (let i = 0; i < list.length; i++) {
	        if (!type(list[i])) {
	            return false;
	        }
	    }
	    return true;
	};

	let exist = (list, type) => {
	    if (!likeArray(list)) {
	        throw new TypeError(typeErrorText(list, 'array'));
	    }
	    if (!isFunction(type)) {
	        throw new TypeError(typeErrorText(type, 'function'));
	    }

	    for (let i = 0; i < list.length; i++) {
	        if (type(list[i])) {
	            return true;
	        }
	    }
	    return false;
	};

	let mapType = (map) => {
	    if (!isObject(map)) {
	        throw new TypeError(typeErrorText(map, 'obj'));
	    }

	    for (let name in map) {
	        let type = map[name];
	        if (!isFunction(type)) {
	            throw new TypeError(typeErrorText(type, 'function'));
	        }
	    }

	    return (v) => {
	        if (!isObject(v)) {
	            return false;
	        }

	        for (let name in map) {
	            let type = map[name];
	            let attr = v[name];
	            if (!type(attr)) {
	                return false;
	            }
	        }

	        return true;
	    };
	};

	let listType = (type) => {
	    if (!isFunction(type)) {
	        throw new TypeError(typeErrorText(type, 'function'));
	    }

	    return (list) => any(list, type);
	};

	let typeErrorText = (v, expect) => {
	    return `Expect ${expect} type, but got type ${typeof v}, and value is ${v}`;
	};

	module.exports = {
	    isArray,
	    likeArray,
	    isString,
	    isObject,
	    isFunction,
	    isNumber,
	    isBool,
	    isNode,
	    isPromise,
	    isNull,
	    isUndefined,
	    isFalsy,
	    isRegExp,
	    isReadableStream,
	    isWritableStream,

	    funType,
	    any,
	    exist,

	    and,
	    or,
	    not,
	    mapType,
	    listType
	};


/***/ },
/* 5 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__(6);


/***/ },
/* 6 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	let idgener = __webpack_require__(7);

	let messageQueue = () => {
	    let queue = {};

	    return {
	        produce: (source) => {
	            let id = idgener();

	            return {
	                data: {
	                    id, source,
	                    time: new Date().getTime()
	                },
	                result: new Promise((resolve, reject) => {
	                    queue[id] = {
	                        resolve,
	                        reject
	                    };
	                })
	            };
	        },

	        consume: ({
	            id,
	            error,
	            data
	        }) => {
	            let item = queue[id];
	            if (error) {
	                item && item.reject(error);
	            } else {
	                item && item.resolve(data);
	            }
	            delete queue[id];
	        }
	    };
	};

	module.exports = messageQueue;


/***/ },
/* 7 */
/***/ function(module, exports, __webpack_require__) {

	module.exports = __webpack_require__(8);


/***/ },
/* 8 */
/***/ function(module, exports) {

	'use strict';

	let count = 0;

	module.exports = ({
	    timeVisual = false
	} = {}) => {
	    count++;
	    if (count > 10e6) {
	        count = 0;
	    }
	    let rand = Math.random(Math.random()) + '';

	    let time = timeVisual ? getTimeStr() : new Date().getTime();

	    return `${time}-${count}-${rand}`;
	};

	let getTimeStr = () => {
	    let date = new Date();
	    return `${date.getFullYear()}_${date.getMonth()+1}_${date.getDate()}_${date.getHours()}_${date.getMinutes()}_${date.getSeconds()}_${date.getMilliseconds()}`;
	};


/***/ },
/* 9 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	let {
	    get
	} = __webpack_require__(10);

	let apply = (fun, args) => {
	    try {
	        return fun.apply(undefined, args);
	    } catch(err) {
	        return err;
	    }
	};

	module.exports = (map, name, args) => {
	    let fun = get(map, name);
	    if (!fun && typeof fun !== 'function') {
	        return new Error(`missing function ${name}`);
	    } else {
	        return apply(fun, args);
	    }
	};


/***/ },
/* 10 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	let {
	    reduce
	} = __webpack_require__(11);
	let {
	    funType, isObject, or, isString, isFalsy
	} = __webpack_require__(4);

	let defineProperty = (obj, key, opts) => {
	    if (Object.defineProperty) {
	        Object.defineProperty(obj, key, opts);
	    } else {
	        obj[key] = opts.value;
	    }
	    return obj;
	};

	let hasOwnProperty = (obj, key) => {
	    if (obj.hasOwnProperty) {
	        return obj.hasOwnProperty(key);
	    }
	    for (var name in obj) {
	        if (name === key) return true;
	    }
	    return false;
	};

	let toArray = (v = []) => Array.prototype.slice.call(v);

	/**
	 * a.b.c
	 */
	let get = funType((sandbox, name = '') => {
	    name = name.trim();
	    let parts = !name ? [] : name.split('.');
	    return reduce(parts, getValue, sandbox, invertLogic);
	}, [
	    isObject,
	    or(isString, isFalsy)
	]);

	let getValue = (obj, key) => obj[key];

	let invertLogic = v => !v;

	let set = (sandbox, name = '', value) => {
	    name = name.trim();
	    let parts = !name ? [] : name.split('.');
	    let parent = sandbox;
	    if (!isObject(parent)) return;
	    if (!parts.length) return;
	    for (let i = 0; i < parts.length - 1; i++) {
	        let part = parts[i];
	        parent = parent[part];
	        // avoid exception
	        if (!isObject(parent)) return null;
	    }

	    parent[parts[parts.length - 1]] = value;
	    return true;
	};

	/**
	 * provide property:
	 *
	 * 1. read props freely
	 *
	 * 2. change props by provide token
	 */

	let authProp = (token) => {
	    let set = (obj, key, value) => {
	        let temp = null;

	        if (!hasOwnProperty(obj, key)) {
	            defineProperty(obj, key, {
	                enumerable: false,
	                configurable: false,
	                set: (value) => {
	                    if (isObject(value)) {
	                        if (value.token === token) {
	                            // save
	                            temp = value.value;
	                        }
	                    }
	                },
	                get: () => {
	                    return temp;
	                }
	            });
	        }

	        setProp(obj, key, value);
	    };

	    let setProp = (obj, key, value) => {
	        obj[key] = {
	            token,
	            value
	        };
	    };

	    return {
	        set
	    };
	};

	let evalCode = (code) => {
	    if (typeof code !== 'string') return code;
	    return eval(`(function(){
	    try {
	        ${code}
	    } catch(err) {
	        console.log('Error happened, when eval code.');
	        throw err;
	    }
	})()`);
	};

	let delay = (time) => new Promise((resolve) => {
	    setTimeout(resolve, time);
	});

	let runSequence = (list, params = [], context, stopV) => {
	    if (!list.length) {
	        return Promise.resolve();
	    }
	    let fun = list[0];
	    let v = fun && fun.apply(context, params);
	    if (stopV && v === stopV) {
	        return Promise.resolve(stopV);
	    }
	    return Promise.resolve(v).then(() => {
	        return runSequence(list.slice(1), params, context, stopV);
	    });
	};

	module.exports = {
	    defineProperty,
	    hasOwnProperty,
	    toArray,
	    get,
	    set,
	    authProp,
	    evalCode,
	    delay,
	    runSequence
	};


/***/ },
/* 11 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	let {
	    isObject, funType, or, isString, isFalsy, likeArray
	} = __webpack_require__(4);

	let iterate = __webpack_require__(12);

	let {
	    map, reduce, find, findIndex, forEach, filter, any, exist, compact
	} = __webpack_require__(13);

	let contain = (list, item, fopts) => findIndex(list, item, fopts) !== -1;

	let difference = (list1, list2, fopts) => {
	    return reduce(list1, (prev, item) => {
	        if (!contain(list2, item, fopts) &&
	            !contain(prev, item, fopts)) {
	            prev.push(item);
	        }
	        return prev;
	    }, []);
	};

	let union = (list1, list2, fopts) => deRepeat(list2, fopts, deRepeat(list1, fopts));

	let mergeMap = (map1 = {}, map2 = {}) => reduce(map2, setValueKey, reduce(map1, setValueKey, {}));

	let setValueKey = (obj, value, key) => {
	    obj[key] = value;
	    return obj;
	};

	let interset = (list1, list2, fopts) => {
	    return reduce(list1, (prev, cur) => {
	        if (contain(list2, cur, fopts)) {
	            prev.push(cur);
	        }
	        return prev;
	    }, []);
	};

	let deRepeat = (list, fopts, init = []) => {
	    return reduce(list, (prev, cur) => {
	        if (!contain(prev, cur, fopts)) {
	            prev.push(cur);
	        }
	        return prev;
	    }, init);
	};

	/**
	 * a.b.c
	 */
	let get = funType((sandbox, name = '') => {
	    name = name.trim();
	    let parts = !name ? [] : name.split('.');
	    return reduce(parts, getValue, sandbox, invertLogic);
	}, [
	    isObject,
	    or(isString, isFalsy)
	]);

	let getValue = (obj, key) => obj[key];

	let invertLogic = v => !v;

	let delay = (time) => new Promise((resolve) => {
	    setTimeout(resolve, time);
	});

	let flat = (list) => {
	    if (likeArray(list) && !isString(list)) {
	        return reduce(list, (prev, item) => {
	            prev = prev.concat(flat(item));
	            return prev;
	        }, []);
	    } else {
	        return [list];
	    }
	};

	module.exports = {
	    flat,
	    contain,
	    difference,
	    union,
	    interset,
	    map,
	    reduce,
	    iterate,
	    find,
	    findIndex,
	    deRepeat,
	    forEach,
	    filter,
	    any,
	    exist,
	    get,
	    delay,
	    mergeMap,
	    compact
	};


/***/ },
/* 12 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	let {
	    likeArray, isObject, funType, isFunction, isUndefined, or, isNumber, isFalsy, mapType
	} = __webpack_require__(4);

	/**
	 *
	 * preidcate: chose items to iterate
	 * limit: when to stop iteration
	 * transfer: transfer item
	 * output
	 */
	let iterate = funType((domain = [], opts = {}) => {
	    let {
	        predicate, transfer, output, limit, def
	    } = opts;

	    opts.predicate = predicate || truthy;
	    opts.transfer = transfer || id;
	    opts.output = output || toList;
	    if (limit === undefined) limit = domain && domain.length;
	    limit = opts.limit = stopCondition(limit);

	    let rets = def;
	    let count = 0;

	    if (likeArray(domain)) {
	        for (let i = 0; i < domain.length; i++) {
	            let itemRet = iterateItem(domain, i, count, rets, opts);
	            rets = itemRet.rets;
	            count = itemRet.count;
	            if (itemRet.stop) return rets;
	        }
	    } else if (isObject(domain)) {
	        for (let name in domain) {
	            let itemRet = iterateItem(domain, name, count, rets, opts);
	            rets = itemRet.rets;
	            count = itemRet.count;
	            if (itemRet.stop) return rets;
	        }
	    }

	    return rets;
	}, [
	    or(isObject, isFunction, isFalsy),
	    or(isUndefined, mapType({
	        predicate: or(isFunction, isFalsy),
	        transfer: or(isFunction, isFalsy),
	        output: or(isFunction, isFalsy),
	        limit: or(isUndefined, isNumber, isFunction)
	    }))
	]);

	let iterateItem = (domain, name, count, rets, {
	    predicate, transfer, output, limit
	}) => {
	    let item = domain[name];
	    if (limit(rets, item, name, domain, count)) {
	        // stop
	        return {
	            stop: true,
	            count,
	            rets
	        };
	    }

	    if (predicate(item)) {
	        rets = output(rets, transfer(item, name, domain, rets), name, domain);
	        count++;
	    }
	    return {
	        stop: false,
	        count,
	        rets
	    };
	};

	let stopCondition = (limit) => {
	    if (isUndefined(limit)) {
	        return falsy;
	    } else if (isNumber(limit)) {
	        return (rets, item, name, domain, count) => count >= limit;
	    } else {
	        return limit;
	    }
	};

	let toList = (prev, v) => {
	    prev.push(v);
	    return prev;
	};

	let truthy = () => true;

	let falsy = () => false;

	let id = v => v;

	module.exports = iterate;


/***/ },
/* 13 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	let iterate = __webpack_require__(12);

	let defauls = {
	    eq: (v1, v2) => v1 === v2
	};

	let setDefault = (opts, defauls) => {
	    for (let name in defauls) {
	        opts[name] = opts[name] || defauls[name];
	    }
	};

	let forEach = (list, handler) => iterate(list, {
	    limit: (rets) => {
	        if (rets === true) return true;
	        return false;
	    },
	    transfer: handler,
	    output: (prev, cur) => cur,
	    def: false
	});

	let map = (list, handler, limit) => iterate(list, {
	    transfer: handler,
	    def: [],
	    limit
	});

	let reduce = (list, handler, def, limit) => iterate(list, {
	    output: handler,
	    def,
	    limit
	});

	let filter = (list, handler, limit) => reduce(list, (prev, cur, index, list) => {
	    handler && handler(cur, index, list) && prev.push(cur);
	    return prev;
	}, [], limit);

	let find = (list, item, fopts) => {
	    let index = findIndex(list, item, fopts);
	    if (index === -1) return undefined;
	    return list[index];
	};

	let any = (list, handler) => reduce(list, (prev, cur, index, list) => {
	    let curLogic = handler && handler(cur, index, list);
	    return prev && originLogic(curLogic);
	}, true, falsyIt);

	let exist = (list, handler) => reduce(list, (prev, cur, index, list) => {
	    let curLogic = handler && handler(cur, index, list);
	    return prev || originLogic(curLogic);
	}, false, originLogic);

	let findIndex = (list, item, fopts = {}) => {
	    setDefault(fopts, defauls);

	    let {
	        eq
	    } = fopts;
	    let predicate = (v) => eq(item, v);
	    let ret = iterate(list, {
	        transfer: indexTransfer,
	        limit: onlyOne,
	        predicate,
	        def: []
	    });
	    if (!ret.length) return -1;
	    return ret[0];
	};

	let compact = (list) => reduce(list, (prev, cur) => {
	    if (cur) prev.push(cur);
	    return prev;
	}, []);

	let indexTransfer = (item, index) => index;

	let onlyOne = (rets, item, name, domain, count) => count >= 1;

	let falsyIt = v => !v;

	let originLogic = v => !!v;

	module.exports = {
	    map,
	    forEach,
	    reduce,
	    find,
	    findIndex,
	    filter,
	    any,
	    exist,
	    compact
	};


/***/ },
/* 14 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	let {
	    isPromise
	} = __webpack_require__(4);

	let id = v => v;

	let wrapListen = (listen, send) => {
	    if (!listen) {
	        return (handle) => (data, ret) => {
	            if (!isPromise(ret)) {
	                throw new Error(`there is no listener and response of send is not a promise. response is ${ret}`);
	            }
	            ret.then(handle).catch(err => handle(getError(err, data)));
	        };
	    } else {
	        return (handle, sendHandle = send) => {
	            listen(handle, sendHandle);
	            return id;
	        };
	    }
	};

	let getError = (err, data) => {
	    return {
	        error: err,
	        id: data.id
	    };
	};

	module.exports = wrapListen;


/***/ },
/* 15 */
/***/ function(module, exports) {

	'use strict';

	let log = console && console.log || (v => v); // eslint-disable-line

	let stringify = (data) => {
	    try {
	        return JSON.stringify(data);
	    } catch (err) {
	        log(`Error happend when stringify data ${data}. Error is ${err}`);
	        throw err;
	    }
	};

	let parseJSON = (str) => {
	    try {
	        return JSON.parse(str);
	    } catch (err) {
	        log(`Error happend when parse json ${str}. Error is ${err}`);
	        throw err;
	    }
	};

	module.exports = {
	    parseJSON,
	    stringify
	};


/***/ }
/******/ ]);