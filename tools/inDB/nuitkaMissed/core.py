"""
certifi.py
~~~~~~~~~~

This module returns the installation location of cacert.pem or its contents.
"""

##############################################################################

#   File "D:\code\satellite-analysis\inDB\inDB.dist\inDB.py", line 22, in <module>
#   File "D:\code\satellite-analysis\inDB\inDB.dist\morecantile\__init__.py", line 14, in <module morecantile>
#   File "D:\code\satellite-analysis\inDB\inDB.dist\morecantile\defaults.py", line 11, in <module morecantile.defaults>
#   File "D:\code\satellite-analysis\inDB\inDB.dist\morecantile\models.py", line 8, in <module morecantile.models>
#   File "D:\code\satellite-analysis\inDB\inDB.dist\pyproj\__init__.py", line 46, in <module pyproj>
#   File "D:\code\satellite-analysis\inDB\inDB.dist\pyproj\network.py", line 8, in <module pyproj.network>
#   File "D:\code\satellite-analysis\inDB\inDB.dist\certifi\__init__.py", line 1, in <module certifi>
#   File "D:\code\satellite-analysis\inDB\inDB.dist\certifi\core.py", line 51, in <module certifi.core>

##############################################################################

import os

def where():
    return os.path.join(os.path.dirname(__file__), "cacert.pem")

def contents():
    with open(where(), encoding="ascii") as f:
        return f.read()

# import sys
# import atexit

# def exit_cacert_ctx() -> None:
#     _CACERT_CTX.__exit__(None, None, None)  # type: ignore[union-attr]


# if sys.version_info >= (3, 11):

#     from importlib.resources import as_file, files

#     _CACERT_CTX = None
#     _CACERT_PATH = None

#     def where() -> str:
#         # This is slightly terrible, but we want to delay extracting the file
#         # in cases where we're inside of a zipimport situation until someone
#         # actually calls where(), but we don't want to re-extract the file
#         # on every call of where(), so we'll do it once then store it in a
#         # global variable.
#         global _CACERT_CTX
#         global _CACERT_PATH
#         if _CACERT_PATH is None:
#             # This is slightly janky, the importlib.resources API wants you to
#             # manage the cleanup of this file, so it doesn't actually return a
#             # path, it returns a context manager that will give you the path
#             # when you enter it and will do any cleanup when you leave it. In
#             # the common case of not needing a temporary file, it will just
#             # return the file system location and the __exit__() is a no-op.
#             #
#             # We also have to hold onto the actual context manager, because
#             # it will do the cleanup whenever it gets garbage collected, so
#             # we will also store that at the global level as well.
#             _CACERT_CTX = as_file(files("certifi").joinpath("cacert.pem"))
#             _CACERT_PATH = str(_CACERT_CTX.__enter__())
#             atexit.register(exit_cacert_ctx)

#         return _CACERT_PATH

#     def contents() -> str:
#         return files("certifi").joinpath("cacert.pem").read_text(encoding="ascii")

# else:

#     from importlib.resources import path as get_path, read_text

#     _CACERT_CTX = None
#     _CACERT_PATH = None

#     def where() -> str:
#         # This is slightly terrible, but we want to delay extracting the
#         # file in cases where we're inside of a zipimport situation until
#         # someone actually calls where(), but we don't want to re-extract
#         # the file on every call of where(), so we'll do it once then store
#         # it in a global variable.
#         global _CACERT_CTX
#         global _CACERT_PATH
#         if _CACERT_PATH is None:
#             # This is slightly janky, the importlib.resources API wants you
#             # to manage the cleanup of this file, so it doesn't actually
#             # return a path, it returns a context manager that will give
#             # you the path when you enter it and will do any cleanup when
#             # you leave it. In the common case of not needing a temporary
#             # file, it will just return the file system location and the
#             # __exit__() is a no-op.
#             #
#             # We also have to hold onto the actual context manager, because
#             # it will do the cleanup whenever it gets garbage collected, so
#             # we will also store that at the global level as well.
#             _CACERT_CTX = get_path("certifi", "cacert.pem")
#             _CACERT_PATH = str(_CACERT_CTX.__enter__())
#             atexit.register(exit_cacert_ctx)

#         return _CACERT_PATH

#     def contents() -> str:
#         return read_text("certifi", "cacert.pem", encoding="ascii")
