<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>文件浏览</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="goBack">返回</el-button>
      </div>

      <!-- 面包屑导航 -->
      <el-breadcrumb class="breadcrumb-container" separator="/">
        <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index"
                            @click.native="handleBreadcrumbClick(item)">
          <span :class="{'breadcrumb-link': index !== breadcrumbs.length - 1}">{{ item.name }}</span>
        </el-breadcrumb-item>
      </el-breadcrumb>

      <el-row :gutter="20" class="main-content">
        <!-- 文件列表部分 -->
        <el-col :span="10">
          <el-card class="file-list-card" shadow="never">
            <div slot="header" class="clearfix">
              <span>文件列表</span>
              <div style="float: right">
                <el-button-group>
                  <el-button icon="el-icon-upload" size="mini" type="primary" @click="handleUpload">上传</el-button>
                  <el-button icon="el-icon-download" size="mini" type="success" @click="handleDownloadFromUrl">
                    从URL下载
                  </el-button>
                  <el-button icon="el-icon-refresh" size="mini" type="info" @click="handleRefresh">刷新</el-button>
                </el-button-group>
              </div>
            </div>
            <el-table v-loading="loading" :data="fileList" height="calc(100vh - 280px)" @row-click="handleRowClick">
              <el-table-column label="文件名" min-width="200" prop="name">
                <template slot-scope="scope">
                  <i :class="scope.row.isDirectory ? 'el-icon-folder' : 'el-icon-document'" style="margin-right: 5px"/>
                  {{ scope.row.name }}
                </template>
              </el-table-column>
              <el-table-column align="right" label="大小" width="120">
                <template slot-scope="scope">
                  {{ formatFileSize(scope.row.totalSpace) }}
                </template>
              </el-table-column>
              <el-table-column align="center" fixed="right" label="操作" width="150">
                <template slot-scope="scope">
                  <el-button
                    v-if="!scope.row.isDirectory"
                    icon="el-icon-download"
                    size="mini"
                    type="text"
                    @click.stop="handleDownload(scope.row)"
                  >下载
                  </el-button>
                  <el-button
                    v-if="!scope.row.isDirectory && isTextFile(scope.row.name)"
                    icon="el-icon-edit"
                    size="mini"
                    type="text"
                    @click.stop="handleEdit(scope.row)"
                  >编辑
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <!-- 预览/编辑部分 -->
        <el-col :span="14">
          <el-card v-if="currentFile" class="preview-card" shadow="never">
            <div slot="header" class="clearfix">
              <span>{{ currentFile.name }}</span>
              <el-button-group style="float: right">
                <el-button
                  v-if="isEditing"
                  size="mini"
                  type="primary"
                  @click="handleSave"
                >保存
                </el-button>
                <el-button
                  v-if="isEditing"
                  size="mini"
                  @click="handleCancel"
                >取消
                </el-button>
              </el-button-group>
            </div>
            <!-- Monaco Editor -->
            <div v-if="isTextFile(currentFile.name)" class="editor-container">
              <monaco-editor
                v-if="isEditing"
                v-model="fileContent"
                :options="editorOptions"
                @change="onEditorChange"
              />
              <pre v-else class="preview-content">{{ fileContent }}</pre>
            </div>
            <!-- 图片预览 -->
            <div v-else-if="isImageFile(currentFile.name)" class="image-preview">
              <img :alt="currentFile.name" :src="previewUrl" style="max-width: 100%;"/>
            </div>
            <!-- 音频预览 -->
            <div v-else-if="isAudioFile(currentFile.name)" class="media-preview">
              <audio controls style="width: 100%;">
                <source :src="previewUrl" :type="getMimeType(currentFile.name)">
                您的浏览器不支持音频播放
              </audio>
            </div>
            <!-- 视频预览 -->
            <div v-else-if="isVideoFile(currentFile.name)" class="media-preview">
              <video controls style="max-width: 100%;">
                <source :src="previewUrl" :type="getMimeType(currentFile.name)">
                您的浏览器不支持视频播放
              </video>
            </div>
            <!-- PDF预览 -->
            <div v-else-if="isPdfFile(currentFile.name)" class="pdf-preview">
              <iframe :src="previewUrl" frameborder="0" height="100%" width="100%"></iframe>
            </div>
            <!-- 其他文件类型 -->
            <div v-else class="file-info">
              <p>此文件类型不支持预览</p>
              <p>文件大小: {{ formatFileSize(currentFile.totalSpace) }}</p>
            </div>
          </el-card>
          <el-empty v-else description="请选择要预览的文件"></el-empty>
        </el-col>
      </el-row>
    </el-card>

    <!-- 上传文件对话框 -->
    <el-dialog :visible.sync="uploadDialogVisible" append-to-body title="上传文件" width="500px">
      <el-upload
        :action="uploadUrl"
        :before-upload="beforeUpload"
        :data="uploadData"
        :headers="headers"
        :on-error="handleUploadError"
        :on-success="handleUploadSuccess"
        class="upload-demo"
        drag
        multiple
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div slot="tip" class="el-upload__tip">支持任意类型文件</div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button @click="uploadDialogVisible = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 从URL下载对话框 -->
    <el-dialog :visible.sync="downloadUrlDialogVisible" append-to-body title="从URL下载文件" width="500px">
      <el-form ref="downloadUrlForm" :model="downloadUrlForm" :rules="downloadUrlRules" label-width="80px">
        <el-form-item label="URL" prop="url">
          <el-input v-model="downloadUrlForm.url" placeholder="请输入文件URL"></el-input>
        </el-form-item>
        <el-form-item label="文件名" prop="filename">
          <el-input v-model="downloadUrlForm.filename" placeholder="请输入保存的文件名（可选）"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="downloadUrlDialogVisible = false">取 消</el-button>
        <el-button :loading="downloadUrlLoading" type="primary" @click="submitDownloadUrl">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {downloadFile, downloadFromUrl, getFileList, saveFile} from "@/api/node/server";
import MonacoEditor from 'monaco-editor-vue';
import {getToken} from "@/utils/auth";

export default {
  name: "FileBrowser",
  components: {
    MonacoEditor
  },
  data() {
    return {
      loading: false,
      serverId: null,
      currentPath: "",
      fileList: [],
      breadcrumbs: [],
      currentFile: null,
      fileContent: '',
      isEditing: false,
      editorOptions: {
        theme: 'vs-dark',
        language: 'plaintext',
        automaticLayout: true,
        minimap: {
          enabled: true
        }
      },
      previewUrl: '',
      // 上传相关
      uploadDialogVisible: false,
      uploadUrl: process.env.VUE_APP_BASE_API + "/node/server/upload",
      headers: {
        Authorization: 'Bearer ' + getToken()
      },
      uploadData: {
        id: null,
        path: ""
      },
      // 从URL下载相关
      downloadUrlDialogVisible: false,
      downloadUrlForm: {
        url: "",
        filename: ""
      },
      downloadUrlRules: {
        url: [
          {required: true, message: "请输入文件URL", trigger: "blur"},
          {type: "url", message: "请输入有效的URL", trigger: "blur"}
        ]
      },
      downloadUrlLoading: false
    };
  },
  created() {
    this.serverId = parseInt(this.$route.params.id);
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      getFileList({
        id: this.serverId,
        path: this.currentPath
      }).then(response => {
        this.fileList = response.data.files;
        this.updateBreadcrumbs();
        this.loading = false;
      });
    },
    handleFolderClick(row) {
      this.currentPath = row.path;
      this.getList();
    },
    handleBreadcrumbClick(item) {
      if (item.path !== this.currentPath) {
        this.currentPath = item.path;
        this.getList();
      }
    },
    handleDownload(row) {
      this.loading = true;
      downloadFile(this.serverId, row.path).then(response => {
        const blob = new Blob([response], {type: 'application/octet-stream'});
        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = row.name;
        link.click();
        window.URL.revokeObjectURL(link.href);
        this.loading = false;
      }).catch(() => {
        this.loading = false;
        this.$modal.msgError("下载失败");
      });
    },
    updateBreadcrumbs() {
      const paths = this.currentPath.split('\\').filter(p => p);
      this.breadcrumbs = [
        {name: '根目录', path: ''}
      ];

      let currentPath = '';
      paths.forEach(path => {
        currentPath += path + '\\';
        this.breadcrumbs.push({
          name: path,
          path: currentPath
        });
      });
    },
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },
    goBack() {
      this.$router.go(-1);
    },
    isTextFile(filename) {
      const textExtensions = ['.txt', '.json', '.xml', '.html', '.css', '.js', '.md', '.log', '.yml', '.yaml', '.properties', '.conf', '.ini', '.cmd', '.bat', '.sh', '.bash', '.ps1', '.py', '.java', '.c', '.cpp', '.h', '.cs', '.php', '.sql', '.vue', '.ts', '.tsx', '.jsx'];
      return textExtensions.some(ext => filename.toLowerCase().endsWith(ext));
    },
    isImageFile(filename) {
      const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg', '.ico', '.tiff', '.tif'];
      return imageExtensions.some(ext => filename.toLowerCase().endsWith(ext));
    },
    isAudioFile(filename) {
      const audioExtensions = ['.mp3', '.wav', '.ogg', '.flac', '.aac', '.m4a', '.wma'];
      return audioExtensions.some(ext => filename.toLowerCase().endsWith(ext));
    },
    isVideoFile(filename) {
      const videoExtensions = ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.mkv', '.webm', '.m4v'];
      return videoExtensions.some(ext => filename.toLowerCase().endsWith(ext));
    },
    isPdfFile(filename) {
      return filename.toLowerCase().endsWith('.pdf');
    },
    async handleEdit(file) {
      this.currentFile = file;
      this.isEditing = true;
      try {
        const response = await downloadFile(this.serverId, file.path);
        const blob = new Blob([response], {type: 'text/plain'});
        const reader = new FileReader();
        reader.onload = (e) => {
          this.fileContent = e.target.result;
          // 设置编辑器语言
          const ext = file.name.substring(file.name.lastIndexOf('.') + 1);
          this.editorOptions.language = this.getLanguageFromExtension(ext);
        };
        reader.readAsText(blob);
      } catch (error) {
        this.$message.error('加载文件内容失败');
      }
    },
    getLanguageFromExtension(ext) {
      const languageMap = {
        'js': 'javascript',
        'json': 'json',
        'html': 'html',
        'css': 'css',
        'md': 'markdown',
        'xml': 'xml',
        'yml': 'yaml',
        'yaml': 'yaml',
        'properties': 'properties',
        'conf': 'ini',
        'ini': 'ini'
      };
      return languageMap[ext.toLowerCase()] || 'plaintext';
    },
    async handleSave() {
      try {
        this.loading = true;
        const response = await saveFile({
          id: this.serverId,
          path: this.currentFile.path,
          content: this.fileContent
        });

        if (response.code === 200) {
          this.$message.success('保存成功');
          this.isEditing = false;
        } else {
          this.$message.error(response.msg || '保存失败');
        }
        this.loading = false;
      } catch (error) {
        this.loading = false;
        this.$message.error('保存失败：' + (error.message || '未知错误'));
      }
    },
    handleCancel() {
      this.isEditing = false;
      this.fileContent = '';
    },
    onEditorChange(value) {
      this.fileContent = value;
    },
    handleRowClick(row) {
      if (row.isDirectory) {
        this.currentPath = row.path;
        this.getList();
      } else {
        this.currentFile = row;
        this.isEditing = false;
        this.fileContent = '';

        if (this.isTextFile(row.name)) {
          this.handleEdit(row);
        } else if (this.isImageFile(row.name)) {
          this.handleImagePreview(row);
        } else if (this.isAudioFile(row.name) || this.isVideoFile(row.name) || this.isPdfFile(row.name)) {
          this.handleMediaPreview(row);
        }
      }
    },
    async handleImagePreview(file) {
      try {
        this.loading = true;
        const response = await downloadFile(this.serverId, file.path);
        // 根据文件扩展名设置正确的MIME类型
        const ext = file.name.substring(file.name.lastIndexOf('.') + 1).toLowerCase();
        const mimeTypes = {
          'jpg': 'image/jpeg',
          'jpeg': 'image/jpeg',
          'png': 'image/png',
          'gif': 'image/gif',
          'bmp': 'image/bmp',
          'webp': 'image/webp'
        };
        const mimeType = mimeTypes[ext] || 'image/jpeg';

        // 创建blob URL
        const blob = new Blob([response], {type: mimeType});
        if (this.previewUrl) {
          URL.revokeObjectURL(this.previewUrl); // 释放之前的URL
        }
        this.previewUrl = URL.createObjectURL(blob);
        this.loading = false;
      } catch (error) {
        this.loading = false;
        this.$message.error('加载图片失败');
      }
    },
    beforeDestroy() {
      // 组件销毁前清理blob URL
      if (this.previewUrl) {
        URL.revokeObjectURL(this.previewUrl);
      }
    },
    getMimeType(filename) {
      const ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
      const mimeTypes = {
        // 图片
        'jpg': 'image/jpeg',
        'jpeg': 'image/jpeg',
        'png': 'image/png',
        'gif': 'image/gif',
        'bmp': 'image/bmp',
        'webp': 'image/webp',
        'svg': 'image/svg+xml',
        'ico': 'image/x-icon',
        'tiff': 'image/tiff',
        'tif': 'image/tiff',
        // 音频
        'mp3': 'audio/mpeg',
        'wav': 'audio/wav',
        'ogg': 'audio/ogg',
        'flac': 'audio/flac',
        'aac': 'audio/aac',
        'm4a': 'audio/mp4',
        'wma': 'audio/x-ms-wma',
        // 视频
        'mp4': 'video/mp4',
        'avi': 'video/x-msvideo',
        'mov': 'video/quicktime',
        'wmv': 'video/x-ms-wmv',
        'flv': 'video/x-flv',
        'mkv': 'video/x-matroska',
        'webm': 'video/webm',
        'm4v': 'video/mp4',
        // PDF
        'pdf': 'application/pdf'
      };
      return mimeTypes[ext] || 'application/octet-stream';
    },
    async handleMediaPreview(file) {
      try {
        this.loading = true;
        const response = await downloadFile(this.serverId, file.path);

        // 创建blob URL
        const blob = new Blob([response], {type: this.getMimeType(file.name)});
        if (this.previewUrl) {
          URL.revokeObjectURL(this.previewUrl); // 释放之前的URL
        }
        this.previewUrl = URL.createObjectURL(blob);
        this.loading = false;
      } catch (error) {
        this.loading = false;
        this.$message.error('加载媒体文件失败');
      }
    },
    // 刷新文件列表
    handleRefresh() {
      this.getList();
    },
    // 上传文件
    handleUpload() {
      this.uploadData.id = this.serverId;
      this.uploadData.path = this.currentPath.replace(/\\/g, '\\\\');
      this.uploadDialogVisible = true;
    },
    // 上传前检查
    beforeUpload(file) {
      return true;
    },
    // 上传成功回调
    handleUploadSuccess(response, file, fileList) {
      if (response.code === 200) {
        this.$message.success('上传成功');
        this.getList();
      } else {
        this.$message.error(response.msg || '上传失败');
      }
    },
    // 上传失败回调
    handleUploadError(err) {
      this.$message.error('上传失败: ' + (err.message || '未知错误'));
    },
    // 从URL下载
    handleDownloadFromUrl() {
      this.downloadUrlForm.url = '';
      this.downloadUrlForm.filename = '';
      this.downloadUrlDialogVisible = true;
    },
    // 提交从URL下载
    submitDownloadUrl() {
      this.$refs.downloadUrlForm.validate(valid => {
        if (valid) {
          this.downloadUrlLoading = true;
          const params = {
            id: this.serverId,
            path: this.currentPath.replace(/\\/g, '\\\\'),
            url: this.downloadUrlForm.url
          };

          if (this.downloadUrlForm.filename) {
            params.filename = this.downloadUrlForm.filename;
          }

          downloadFromUrl(params).then(response => {
            if (response.code === 200) {
              this.$message.success('下载成功');
              this.getList();
            } else {
              this.$message.error(response.msg || '下载失败');
            }
            this.downloadUrlLoading = false;
            this.downloadUrlDialogVisible = false;
          }).catch(() => {
            this.downloadUrlLoading = false;
            this.$message.error('下载失败');
          });
        }
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 84px);
}

.box-card {
  margin-bottom: 20px;
}

.breadcrumb-container {
  margin-bottom: 20px;
  padding: 10px 0;
  border-bottom: 1px solid #e8e8e8;
}

.breadcrumb-link {
  color: #409EFF;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.main-content {
  margin-top: 20px;
}

.file-list-card, .preview-card {
  height: calc(100vh - 220px);

  .el-card__body {
    height: calc(100% - 60px);
    padding: 0;
  }
}

.editor-container {
  height: calc(100vh - 280px);
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.preview-content {
  padding: 20px;
  background-color: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', monospace;
  white-space: pre-wrap;
  overflow: auto;
  height: calc(100vh - 280px);
  margin: 0;
}

.image-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  height: calc(100vh - 280px);
  background-color: #f5f7fa;

  img {
    max-height: 100%;
    object-fit: contain;
  }
}

.media-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  height: calc(100vh - 280px);
  background-color: #f5f7fa;
  padding: 20px;
}

.pdf-preview {
  height: calc(100vh - 280px);
  background-color: #f5f7fa;

  iframe {
    border: none;
  }
}

.file-info {
  padding: 20px;
  text-align: center;
  color: #909399;
}

:deep(.el-table) {
  height: 100% !important;
}

:deep(.el-card__header) {
  padding: 15px 20px;
  border-bottom: 1px solid #e8e8e8;
}

:deep(.el-empty) {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.upload-demo {
  text-align: center;
}
</style>
