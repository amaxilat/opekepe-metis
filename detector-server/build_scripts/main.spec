# -*- mode: python ; coding: utf-8 -*-

import sys
sys.setrecursionlimit(5000)

block_cipher = None

a = Analysis(['../detector_server.py', '../metrics.py'],
             pathex=[],
             binaries=[],
             datas=[],
             hiddenimports=[
             'engineio.async_drivers.threading',
             'tensorflow.compiler.tf2tensorrt'
             ],
             hookspath=[],
             runtime_hooks=[],
             excludes=[
             'boto3',
             'botocore',
             'matplotlib',
             'notebook',
             'tornado',
             ],
             win_no_prefer_redirects=False,
             win_private_assemblies=False,
             cipher=block_cipher,
             noarchive=False)

pyz = PYZ(a.pure, a.zipped_data,
             cipher=block_cipher)
exe = EXE(pyz,
          a.scripts,
          [],
          exclude_binaries=True,
          name='detector_server',
          debug=False,
          bootloader_ignore_signals=False,
          strip=False,
          upx=True,
          console=False )
coll = COLLECT(exe,
               a.binaries,
               a.zipfiles,
               a.datas,
               strip=False,
               upx=True,
               upx_exclude=[],
               name='detector_server')
